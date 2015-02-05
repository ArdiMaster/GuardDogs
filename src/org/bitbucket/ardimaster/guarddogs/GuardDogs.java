/*
 * Copyright (c) 2015, ArdiMaster
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 *    used to endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.bitbucket.ardimaster.guarddogs;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

/**
 * GuardDogs main class
 *
 * @author ArdiMaster
 */
public class GuardDogs extends JavaPlugin {

    /**
     * The name of the configuration file
     */
    protected String configFileName = "config.yml";
    /** This HashSet holds all the guard dogs */
    protected HashSet<Wolf> guards = new HashSet<>();
    /** This HashMap contains the allocation of guard dogs --> targets */
    protected HashMap<Wolf, LivingEntity> guardTargets = new HashMap<>();
    /** This HashMap contains the allocation of guard dogs --> location */
    protected HashMap<Wolf, Location> guardPositions = new HashMap<>();
    /** This HashMap contains the allocation of guard dogs --> their wait countdown */
    protected HashMap<Wolf, Integer> guardWaits = new HashMap<>();
    /** This HashMap contains the allocation of guard dogs --> their ignored players */
    protected HashMap<Wolf, HashSet<String>> guardIgnores = new HashMap<>();
    /** This HashMap contains the allocation of players --> the guard dogs whose ignores players are currently set */
    protected HashMap<Player, Wolf> settingIgnore = new HashMap<>();
    /** This HashMap contains the allocation of guard dogs --> the amount of extra damage they deal */
    protected HashMap<Wolf, Integer> guardExtraDamage = new HashMap<>();
    /** This HashMap contains the allocation of guard dogs --> the chance of igniting their target upon attack */
    protected HashMap<Wolf, Integer> guardIgniteChance = new HashMap<>();
    /** This HashMap contains the allocation of guard dogs --> the number of times they can teleport */
    protected HashMap<Wolf, Integer> guardTeleportCount = new HashMap<>();
    /** This boolean is true when guard dog targets are currently determined  */
    protected boolean targetDetermination = false;
    /** The materials required to right-click a guard dog with */
    protected Material createMat, disableMat, ignoreMat, extraDamageMat, igniteChanceMat, teleportMat = null;
    /** The most recent version available for download, as determined in onEnable */
    protected String currentVersion = "ERROR";
    /** The repeating Bukkit task for guard dogs target determination */
    private BukkitTask targetDeterminer;
    /** The repeating Bukkit task for guard dogs countdowns */
    private BukkitTask guardTicker;
    /** The Plugin Metrics / MCStats instance */
    private Metrics metrics;

    /** Method ran when plugin gets enabled by server  */
    @Override
    public void onEnable() {
        loadGuards();
        targetDeterminer = new TargetDeterminer(this).runTaskTimer(this, 30 * 20, 10);
        guardTicker = new GuardTicker(this).runTaskTimer(this, 15 * 20, 10);
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        try {
            metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            log(Level.WARNING, "Could not start metrics! Is outbound communication blocked, or is the server offline?");
            e.printStackTrace();
        }

        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            /**
             * Async method invoked by server on plugin initialization in order to determine
             * the most recent available version of the plugin
             */
            @Override
            public void run() {
                try {
                    URL url = new URL("http://files.diepixelecke.tk/GuardDogs/currentVersion.txt");
                    Scanner s = new Scanner(url.openStream());
                    currentVersion = s.nextLine();
                    s.close();
                    if (currentVersion.equals(getDescription().getVersion())) {
                        log(Level.INFO, "Plugin up-to-date.");
                    } else {
                        log(Level.WARNING, "An update was found! The newest version is " + currentVersion +
                                ", you are still running " + getDescription().getVersion() + "! Grab it at " +
                                "http://dev.bukkit.org/bukkit-plugins/guard-dogs");
                    }
                } catch (IOException e) {
                    log(Level.WARNING, "Could not check for updates! Please check manually at " +
                            "http://dev.bukkit.org/bukkit-plugins/guard-dogs");
                    e.printStackTrace();
                }
            }
        });

        log(Level.INFO, "Plugin loaded and available!");
    }

    /** Method ran when plugin gets disabled by server */
    @Override
    public void onDisable() {
        targetDeterminer.cancel();
        guardTicker.cancel();
        saveGuards();
    }

    /**
     * Method invoked to print informational log messages
     *
     * @param message The message to print
     */
    public void log(Level level, String message) {
        getLogger().log(level, message);
    }

    /**
     * This method handles the creation of guard dogs
     *
     * @param wolf The wolf supposed to become a guard dog
     * @param extraDamage How much extra damage the guard dog will deal
     * @param igniteChance The chance the guard dog will ignite his target on attack
     * @param teleports The remaining amount of times the guard dog can teleport back home
     * @return Returns false if the given wolf already is a guard dog, returns true otherwise.
     */
    public boolean createGuard(Wolf wolf, int extraDamage, int igniteChance, int teleports) {
        if (guards.contains(wolf)) {
            return false;
        }

        guards.add(wolf);
        guardPositions.put(wolf, wolf.getLocation());
        guardWaits.put(wolf, 40);
        guardExtraDamage.put(wolf, extraDamage);
        guardIgniteChance.put(wolf, igniteChance);
        guardTeleportCount.put(wolf, teleports);
        wolf.setSitting(true);
        wolf.setCollarColor(DyeColor.LIME);
        wolf.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 1));
        log(Level.INFO, "A new guard dog has been created: " + wolf.getUniqueId().toString());
        saveGuards();
        return true;
    }

    /**
     * Method invoked when a wolf dies
     *
     * @param wolf The wolf which died
     */
    public void deadGuard(Wolf wolf) {
        if (!guards.contains(wolf)) {
            return;
        }

        guards.remove(wolf);
        guardPositions.remove(wolf);
        guardExtraDamage.remove(wolf);
        guardIgniteChance.remove(wolf);
        guardTeleportCount.remove(wolf);

        if (guardTargets.containsKey(wolf)) {
            guardTargets.remove(wolf);
        }

        if (guardIgnores.containsKey(wolf)) {
            guardIgnores.remove(wolf);
        }

        if (guardWaits.containsKey(wolf)) {
            guardWaits.remove(wolf);
        }

        log(Level.INFO, "A guard dog has died: " + wolf.getUniqueId().toString());
        saveGuards();
        if (wolf.getOwner() instanceof Player) {
            Player player = (Player) wolf.getOwner();
            if (player.isOnline()) {
                player.sendMessage(ChatColor.RED + "One of your " + ChatColor.DARK_GREEN + "Guard Dogs" +
                        ChatColor.RED + " has died.");
            }
        }
    }

    /**
     * Method invoked when a guard dog is requested to be removed / disabled
     *
     * @param wolf The wolf requested to be removed
     * @param player Currently unused, will be removed soon.
     * @return Whether the requested wolf was a guard dog. (true if he was)
     */
    public boolean removeGuard(Wolf wolf, Player player) {
        if (!guards.contains(wolf)) {
            return false;
        }

        guards.remove(wolf);
        guardPositions.remove(wolf);
        guardExtraDamage.remove(wolf);
        guardIgniteChance.remove(wolf);
        guardTeleportCount.remove(wolf);

        if (guardTargets.containsKey(wolf)) {
            guardTargets.remove(wolf);
        }

        if (guardIgnores.containsKey(wolf)) {
            guardIgnores.remove(wolf);
        }

        if (guardWaits.containsKey(wolf)) {
            guardWaits.remove(wolf);
        }

        log(Level.INFO, "A guard dog has been removed: " + wolf.getUniqueId().toString());
        saveGuards();
        return true;
    }

    /** Method invoked to save all configuration and guard dogs to disk */
    public void saveGuards() {
        File configFile = new File(getDataFolder(), configFileName);
        if (configFile.exists()) {
            configFile.delete();
        }

        try {
            if (!Files.exists(getDataFolder().toPath())) {
                Files.createDirectory(getDataFolder().toPath());
            }
            configFile.createNewFile();
        } catch (IOException e) {
            log(Level.WARNING, "Unable to create config file!");
            e.printStackTrace();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        List<String> guardIds = new ArrayList<>();

        for (Wolf wolf : guards) {
            String id = wolf.getUniqueId().toString();
            Location loc = guardPositions.get(wolf);
            if (loc == null) {
                log(Level.SEVERE, "Something is bad");
                throw new AssertionError("Attempting to save a guard dog whose position is null! Something went " +
                        "terribly wrong here.");
            }
            guardIds.add(id);
            config.set("guards." + id + ".world", loc.getWorld().getName());
            config.set("guards." + id + ".X", loc.getBlockX());
            config.set("guards." + id + ".Y", loc.getBlockY());
            config.set("guards." + id + ".Z", loc.getBlockZ());
            config.set("guards." + id + ".extraDamage", guardExtraDamage.get(wolf));
            config.set("guards." + id + ".igniteChance", guardIgniteChance.get(wolf));
            config.set("guards." + id + ".teleports", guardTeleportCount.get(wolf));

            if (guardIgnores.containsKey(wolf)) {
                ArrayList<String> ignores = new ArrayList<>();
                for (String s : guardIgnores.get(wolf)) {
                    ignores.add(s);
                }
                config.set("guards." + id + ".ignores", ignores);
            }
        }

        config.set("id.create", createMat.toString());
        config.set("id.disable", disableMat.toString());
        config.set("id.ignore", ignoreMat.toString());
        config.set("id.extaDamage", extraDamageMat.toString());
        config.set("id.igniteChance", igniteChanceMat.toString());
        config.set("id.teleport", teleportMat.toString());

        config.set("guards.guardids", guardIds);
        config.set("version", getDescription().getVersion());
        try {
            config.save(configFile);
        } catch (IOException e) {
            log(Level.WARNING, "Unable to save config file!");
        }

    }

    /** Method invoked to load all config and guard dogs from disk */
    protected void loadGuards() {
        boolean newConfig = true;
        File configFile = new File(getDataFolder(), configFileName);
        if (!configFile.exists()) {
            log(Level.INFO, "No config file.");
            createMat = Material.PUMPKIN_SEEDS;
            disableMat = Material.STICK;
            ignoreMat = Material.GOLD_NUGGET;
            extraDamageMat = Material.DIAMOND;
            igniteChanceMat = Material.BLAZE_POWDER;
            teleportMat = Material.ENDER_PEARL;
            if (Files.exists(new File(getDataFolder(), "guards.yml").toPath())) {
                log(Level.INFO, "Found old guards.yml, renaming...");
                try {
                    Files.move(new File(getDataFolder(), "guards.yml").toPath(), new File(getDataFolder(),
                            configFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log(Level.INFO, "Succeeded, proceeding to load old configuration scheme (will be converted on " +
                            "next save)");
                    newConfig = false;
                } catch (IOException e) {
                    log(Level.WARNING, "Unable to rename guards.yml to config.yml, proceeding with empty config.");
                    e.printStackTrace();
                }

            } else {
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        List<String> guardIds;
        if (newConfig) {
            guardIds = config.getStringList("guards.guardids");
        } else {
            guardIds = config.getStringList("guards");
        }

        if (newConfig) {
            createMat = Material.getMaterial(config.getString("id.create"));
            disableMat = Material.getMaterial(config.getString("id.disable"));
            ignoreMat = Material.getMaterial(config.getString("id.ignore"));
            extraDamageMat = Material.getMaterial(config.getString("id.extraDamage"));
            igniteChanceMat = Material.getMaterial(config.getString("id.igniteChance"));
            teleportMat = Material.getMaterial(config.getString("id.teleport"));
        }

        for (World world : getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Wolf) {
                    if (guardIds.contains(entity.getUniqueId().toString())) {
                        Wolf wolf = (Wolf) entity;
                        String uuid = wolf.getUniqueId().toString();

                        World posWorld;
                        int X, Y, Z;
                        if (newConfig) {
                            posWorld = getServer().getWorld(config.getString("guards." + uuid + ".world"));
                            X = Integer.parseInt(config.getString("guards." + uuid + ".X"));
                            Y = Integer.parseInt(config.getString("guards." + uuid + ".Y"));
                            Z = Integer.parseInt(config.getString("guards." + uuid + ".Z"));
                        } else {
                            posWorld = getServer().getWorld((String) config.get(uuid + ".world"));
                            X = Integer.parseInt((String) config.get(uuid + ".X"));
                            Y = Integer.parseInt((String) config.get(uuid + ".Y"));
                            Z = Integer.parseInt((String) config.get(uuid + ".Z"));
                        }
                        Location pos = new Location(posWorld, X, Y, Z);
                        entity.teleport(pos);
                        createGuard(wolf, 0, 0, 0);

                        if (newConfig) {
                            if (config.contains("guards." + uuid + ".ignores")) {
                                List<String> ignores = config.getStringList(uuid + ".ignores");
                                HashSet<String> putIgnores = new HashSet<>();
                                for (String s : ignores) {
                                    putIgnores.add(s);
                                }
                                guardIgnores.put(wolf, putIgnores);
                            }
                        } else {
                            if (config.contains(uuid + ".ignores")) {
                                List<String> ignores = config.getStringList(uuid + ".ignores");
                                HashSet<String> putIgnores = new HashSet<>();
                                for (String s : ignores) {
                                    putIgnores.add(s);
                                }
                                guardIgnores.put(wolf, putIgnores);
                            }
                        }

                    }
                }
            }
        }
        log(Level.INFO, "Loading of config completed.");
    }

    /** Method invoked by server when a command is ran */
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("guarddogs")) {
            if (sender instanceof Player) {
                if (!sender.hasPermission("guarddogs.admin")) {
                    sender.sendMessage(ChatColor.RED + "Sorry, but you don't have permission to edit guard dogs iems.");
                    return true;
                }
            }

            if (args.length == 0) {
                sender.sendMessage(ChatColor.GREEN + "You are running " + ChatColor.DARK_GREEN + "Guard Dogs" +
                        ChatColor.GREEN + " version " + ChatColor.AQUA + getDescription().getVersion());
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /guarddogs [create|disable|ignore] [Material]" +
                        ChatColor.GRAY + " - " + ChatColor.GREEN + "Changes the item you need to right click a wolf " +
                        "with in order to perform the specified action.");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                Material tmp = Material.getMaterial(args[1]);
                if (tmp != null) {
                    createMat = tmp;
                    sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a tamed wolf " +
                            "with " + ChatColor.AQUA + args[1] + ChatColor.GREEN + " to make it a guard dog.");
                } else {
                    sender.sendMessage(ChatColor.RED + args[1] + " is not a valid material!");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("disable")) {
                Material tmp = Material.getMaterial(args[1]);
                if (tmp != null) {
                    disableMat = tmp;
                    sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard dog " +
                            "with " + ChatColor.AQUA + args[1] + ChatColor.GREEN + " to disable it.");
                } else {
                    sender.sendMessage(ChatColor.RED + args[1] + " is not a valid material!");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("ignore")) {
                Material tmp = Material.getMaterial(args[1]);
                if (tmp != null) {
                    ignoreMat = tmp;
                    sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard dog " +
                            "with " + ChatColor.AQUA + args[1] + ChatColor.GREEN + " to make it ignore a player.");
                } else {
                    sender.sendMessage(ChatColor.RED + args[1] + " is not a valid material!");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /guarddogs [create|disable|ignore] [Material]" +
                        ChatColor.GRAY + " - " + ChatColor.GREEN + "Changes the item you need to right click a wolf " +
                        "with in order to perform the specified action.");
                return true;
            }
        }

        return false;
    }
}
