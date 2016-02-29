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
 * @author ArdiMaster
 */
public class GuardDogs extends JavaPlugin {
    protected String configFileName = "config.yml";
    protected HashSet<Wolf> guards = new HashSet<>();
    protected HashMap<Wolf, LivingEntity> guardTargets = new HashMap<>();
    protected HashMap<Wolf, Location> guardPositions = new HashMap<>();
    protected HashMap<Wolf, Integer> guardWaits = new HashMap<>();
    protected HashMap<Wolf, HashSet<String>> guardIgnores = new HashMap<>();
    protected HashMap<Player, Wolf> settingIgnore = new HashMap<>();
    protected HashMap<Wolf, Integer> guardExtraDamage = new HashMap<>();
    protected HashMap<Wolf, Integer> guardIgniteChance = new HashMap<>();
    protected HashMap<Wolf, Integer> guardTeleportCount = new HashMap<>();
    protected boolean targetDetermination = false;
    protected Material createMat, disableMat, ignoreMat, extraDamageMat, igniteChanceMat, teleportMat = null;
    protected String currentVersion = "ERROR";
    protected boolean notifyUpdates, extraDamage, igniteChance, teleport;
    protected int extraDamageMax, igniteChanceMax, teleportMax;
    private BukkitTask targetDeterminer;
    private BukkitTask guardTicker;
    private Metrics metrics;

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

    @Override
    public void onDisable() {
        targetDeterminer.cancel();
        guardTicker.cancel();
        saveGuards();
    }

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

    public boolean removeGuard(Wolf wolf) {
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
        config.set("id.extraDamage", extraDamageMat.toString());
        config.set("id.igniteChance", igniteChanceMat.toString());
        config.set("id.teleport", teleportMat.toString());
        config.set("special.extraDamage", extraDamage);
        config.set("special.igniteChance", igniteChance);
        config.set("special.teleport", teleport);
        config.set("notifyUpdates", notifyUpdates);

        config.set("guards.guardids", guardIds);
        config.set("version", getDescription().getVersion());
        try {
            config.save(configFile);
        } catch (IOException e) {
            log(Level.WARNING, "Unable to save config file!");
        }

    }

    protected void loadGuards() {
        String configVersion = "unknown";
        File configFile = new File(getDataFolder(), configFileName);
        if (!configFile.exists()) {
            if (Files.exists(new File(getDataFolder(), "guards.yml").toPath())) {
                log(Level.INFO, "Found ultra-old guards.yml, renaming...");
                try {
                    Files.move(new File(getDataFolder(), "guards.yml").toPath(), new File(getDataFolder(),
                            configFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log(Level.INFO, "Succeeded, proceeding to load old configuration scheme (will be converted on " +
                            "next save)");
                    configVersion = "0.5.6";
                } catch (IOException e) {
                    log(Level.WARNING, "Unable to rename guards.yml to config.yml, proceeding with empty config.");
                    e.printStackTrace();
                    createMat = Material.PUMPKIN_SEEDS;
                    disableMat = Material.STICK;
                    ignoreMat = Material.GOLD_NUGGET;

                    extraDamageMat = Material.DIAMOND;
                    igniteChanceMat = Material.BLAZE_POWDER;
                    teleportMat = Material.ENDER_PEARL;

                    extraDamage = true;
                    igniteChance = true;
                    teleport = true;
                    extraDamageMax = 2;
                    igniteChanceMax = 6;
                    teleportMax = 16;

                    notifyUpdates = true;
                    return;
                }

            } else {
                log(Level.INFO, "No config file.");
                createMat = Material.PUMPKIN_SEEDS;
                disableMat = Material.STICK;
                ignoreMat = Material.GOLD_NUGGET;

                extraDamageMat = Material.DIAMOND;
                igniteChanceMat = Material.BLAZE_POWDER;
                teleportMat = Material.ENDER_PEARL;

                extraDamage = true;
                igniteChance = true;
                teleport = true;
                extraDamageMax = 2;
                igniteChanceMax = 6;
                teleportMax = 16;

                notifyUpdates = true;
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (configVersion.equals("unkown")) {
            configVersion = config.getString("version");
        }

        List<String> guardIds;
        switch (configVersion) {
            case "0.5.6":
                guardIds = config.getStringList("guards");
                break;
            default:
                guardIds = config.getStringList("guards.guardids");
                break;
        }

        switch (configVersion) {
            case "0.5.6":
                createMat = Material.PUMPKIN_SEEDS;
                disableMat = Material.STICK;
                ignoreMat = Material.GOLD_NUGGET;

                extraDamageMat = Material.DIAMOND;
                igniteChanceMat = Material.BLAZE_POWDER;
                teleportMat = Material.ENDER_PEARL;

                extraDamage = true;
                igniteChance = true;
                teleport = true;
                extraDamageMax = 2;
                igniteChanceMax = 6;
                teleportMax = 16;

                notifyUpdates = true;
                break;
            case "0.6":
            case "0.6.5":
                createMat = Material.getMaterial(config.getString("id.create"));
                disableMat = Material.getMaterial(config.getString("id.disable"));
                ignoreMat = Material.getMaterial(config.getString("id.ignore"));

                extraDamageMat = Material.DIAMOND;
                igniteChanceMat = Material.BLAZE_POWDER;
                teleportMat = Material.ENDER_PEARL;

                extraDamage = true;
                igniteChance = true;
                teleport = true;
                extraDamageMax = 2;
                igniteChanceMax = 6;
                teleportMax = 16;

                notifyUpdates = true;
                break;
            default:
                createMat = Material.getMaterial(config.getString("id.create"));
                disableMat = Material.getMaterial(config.getString("id.disable"));
                ignoreMat = Material.getMaterial(config.getString("id.ignore"));

                extraDamageMat = Material.getMaterial(config.getString("id.extraDamage"));
                igniteChanceMat = Material.getMaterial(config.getString("id.igniteChance"));
                teleportMat = Material.getMaterial(config.getString("id.teleport"));

                extraDamage = config.getBoolean("special.extraDamage");
                igniteChance = config.getBoolean("special.igniteChance");
                teleport = config.getBoolean("special.teleport");
                extraDamageMax = config.getInt("special.extraDamageMax");
                igniteChanceMax = config.getInt("special.igniteChanceMax");
                teleportMax = config.getInt("special.teleportMax");

                notifyUpdates = config.getBoolean("notifyUpdates");
                break;
        }

        for (World world : getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Wolf) {
                    if (guardIds.contains(entity.getUniqueId().toString())) {
                        Wolf wolf = (Wolf) entity;
                        String uuid = wolf.getUniqueId().toString();

                        World posWorld;
                        int X, Y, Z, extraDamage, igniteChance, teleports;

                        switch (configVersion) {
                            case "0.5.6":
                                posWorld = getServer().getWorld((String) config.get(uuid + ".world"));
                                X = Integer.parseInt((String) config.get(uuid + ".X"));
                                Y = Integer.parseInt((String) config.get(uuid + ".Y"));
                                Z = Integer.parseInt((String) config.get(uuid + ".Z"));
                                extraDamage = 0;
                                igniteChance = 0;
                                teleports = 0;
                                break;
                            default:
                                posWorld = getServer().getWorld(config.getString("guards." + uuid + ".world"));
                                X = config.getInt("guards." + uuid + ".X");
                                Y = config.getInt("guards." + uuid + ".Y");
                                Z = config.getInt("guards." + uuid + ".Z");
                                switch (configVersion) {
                                    case "0.6":
                                    case "0.6.5":
                                        extraDamage = 0;
                                        igniteChance = 0;
                                        teleports = 0;
                                        break;
                                    default:
                                        extraDamage = config.getInt("guards." + uuid + ".extraDamage");
                                        igniteChance = config.getInt("guards." + uuid + ".igniteChance");
                                        teleports = config.getInt("guards." + uuid + ".teleports");
                                        break;
                                }
                                break;
                        }
                        Location pos = new Location(posWorld, X, Y, Z);
                        entity.teleport(pos);
                        createGuard(wolf, extraDamage, igniteChance, teleports);

                        switch (configVersion) {
                            case "0.5.6":
                                if (config.contains(uuid + ".ignores")) {
                                    List<String> ignores = config.getStringList(uuid + ".ignores");
                                    HashSet<String> putIgnores = new HashSet<>();
                                    for (String s : ignores) {
                                        putIgnores.add(s);
                                    }
                                    guardIgnores.put(wolf, putIgnores);
                                }
                                break;
                            default:
                                if (config.contains("guards." + uuid + ".ignores")) {
                                    List<String> ignores = config.getStringList("guards." + uuid + ".ignores");
                                    HashSet<String> putIgnores = new HashSet<>();
                                    for (String s : ignores) {
                                        putIgnores.add(s);
                                    }
                                    guardIgnores.put(wolf, putIgnores);
                                }
                                break;
                        }

                    }
                }
            }
        }
        log(Level.INFO, "Loading of config [config file of plugin version " + configVersion + "] completed.");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("guarddogs")) {
            if (sender instanceof Player) {
                if (!sender.hasPermission("guarddogs.admin")) {
                    sender.sendMessage(ChatColor.RED + "Sorry, but you don't have permission to use this command.");
                    return true;
                }
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
                if (getDescription().getVersion().equals(currentVersion)) {
                    sender.sendMessage(ChatColor.GREEN + "You are using plugin " + ChatColor.DARK_GREEN +
                            "Guard Dogs" + ChatColor.GREEN + ", version " + ChatColor.AQUA +
                            getDescription().getVersion() + ChatColor.GREEN + ". No updates available.");
                } else if (currentVersion.equals("ERROR")) {
                    sender.sendMessage(ChatColor.GREEN + "You are using plugin " + ChatColor.DARK_GREEN +
                            "Guard Dogs" + ChatColor.GREEN + ", version " + ChatColor.AQUA +
                            getDescription().getVersion() + ChatColor.GREEN + ". An " + ChatColor.RED + "Error" +
                            ChatColor.GREEN + " occured while trying to check for updates, please check manually " +
                            "at" + ChatColor.AQUA + "http://dev.bukkit.org/bukkit-plugins/guard-dogs");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "You are using plugin " + ChatColor.DARK_GREEN +
                            "Guard Dogs" + ChatColor.GREEN + ", version " + ChatColor.AQUA +
                            getDescription().getVersion() + ChatColor.GREEN + ". Version " + ChatColor.AQUA +
                            currentVersion + ChatColor.GREEN + " is available! Grab it at " + ChatColor.AQUA +
                            "http://dev.bukkit.org/bukkit-plugins/guard-dogs");
                }

                return true;
            }

            if (args.length == 0 || (args.length == 1 && args[0].equals("help"))) {
                sendHelp(sender);
                return true;
            }

            if (args.length == 1) {
                switch (args[0]) {
                    case "materials":
                        sender.sendMessage(ChatColor.RED + "/guarddogs materials [create|disable|ignore] [Material] " +
                                ChatColor.GOLD + "- set the material required to perform the specified guard dog action");
                        sender.sendMessage(ChatColor.RED + "/guarddogs materials [extradamage|ignitechance|teleport] " +
                                "[Material]" + ChatColor.GOLD + "- set the material required for the specified guard " +
                                "dog special ability");
                        break;
                    case "enable":
                        sender.sendMessage(ChatColor.RED + "/guarddogs enable notifyupdates" + ChatColor.GOLD +
                                " - enables the on-join update alert for admins");
                        sender.sendMessage(ChatColor.RED + "/guarddogs enable [extradamage|ignitechance|teleport]" +
                                ChatColor.GOLD + " - enables the specified guard dog special ability");
                        break;
                    case "disable":
                        sender.sendMessage(ChatColor.RED + "/guarddogs disable notifyupdates" + ChatColor.GOLD +
                                " - disables the on-join update alert for admins");
                        sender.sendMessage(ChatColor.RED + "/guarddogs disable [extradamage|ignitechance|teleport]" +
                                ChatColor.GOLD + " - disables the specified guard dog special ability");
                        break;
                    default:
                        sendHelp(sender);
                        break;
                }
                return true;
            }

            if (args.length == 2 && args[0].equals("enable")) {
                switch (args[1]) {
                    case "extradamage":
                        extraDamage = true;
                        sender.sendMessage(ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN + "s can now deal " +
                                "extra damage.");
                        break;
                    case "ignitechance":
                        igniteChance = true;
                        sender.sendMessage(ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN + "s can now have a" +
                                "chance to ignite their targets upon attack.");
                        break;
                    case "teleport":
                        teleport = true;
                        sender.sendMessage(ChatColor.DARK_GREEN + "Gaurd Dog" + ChatColor.GREEN + "s can now teleport" +
                                "home when they are low on health");
                        break;
                    case "notifyupdates":
                        notifyUpdates = true;
                        sender.sendMessage(ChatColor.GREEN + "Admins will now be notified about available updates " +
                                "on join.");
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "/guarddogs enable notifyupdates" + ChatColor.GOLD +
                                " - enables the on-join update alert for admins");
                        sender.sendMessage(ChatColor.RED + "/guarddogs enable [extradamage|ignitechance|teleport]" +
                                ChatColor.GOLD + " - enables the specified guard dog special ability");
                }
            }

            if (args.length == 2 && args[0].equals("disable")) {
                switch (args[1]) {
                    case "extradamage":
                        extraDamage = false;
                        sender.sendMessage(ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN + "s can no longer " +
                                "deal extra damage.");
                        break;
                    case "ignitechance":
                        igniteChance = false;
                        sender.sendMessage(ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN + "s can no longer " +
                                "have a chance to ignite their targets upon attack.");
                        break;
                    case "teleport":
                        teleport = false;
                        sender.sendMessage(ChatColor.DARK_GREEN + "Gaurd Dog" + ChatColor.GREEN + "s can now teleport" +
                                "home when they are low on health");
                        break;
                    case "notifyupdates":
                        notifyUpdates = false;
                        sender.sendMessage(ChatColor.GREEN + "Admins will now be notified about available updates " +
                                "on join.");
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "/guarddogs disable notifyupdates" + ChatColor.GOLD +
                                " - enables the on-join update alert for admins");
                        sender.sendMessage(ChatColor.RED + "/guarddogs disable [extradamage|ignitechance|teleport]" +
                                ChatColor.GOLD + " - disable the specified guard dog special ability");
                }
            }

            if (args.length == 3 && args[0].equals("materials")) {
                Material tmp = Material.getMaterial(args[2]);
                switch (args[1]) {
                    case "create":
                        if (tmp != null) {
                            createMat = tmp;
                            sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a tamed " +
                                    "wolf with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to make it a guard dog.");
                        } else {
                            sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material!");
                        }
                        break;
                    case "disable":
                        if (tmp != null) {
                            disableMat = tmp;
                            sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard " +
                                    "dog with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to disable it.");
                        } else {
                            sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material!");
                        }
                        break;
                    case "ignore":
                        if (tmp != null) {
                            ignoreMat = tmp;
                            sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard " +
                                    "dog with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to make it ignore " +
                                    "a player.");
                        } else {
                            sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material!");
                        }
                        break;
                    case "extradamage":
                        if (tmp != null) {
                            extraDamageMat = tmp;
                            sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard " +
                                    "dog with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to make it deal " +
                                    "extra damage.");
                        } else {
                            sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material!");
                        }
                        break;
                    case "ignitechance":
                        if (tmp != null) {
                            igniteChanceMat = tmp;
                            sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard " +
                                    "dog with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to increase its " +
                                    "chance of ignote its enemies.");
                        } else {
                            sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material!");
                        }
                        break;
                    case "teleport":
                        if (tmp != null) {
                            teleportMat = tmp;
                            sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard " +
                                    "dog with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to allow it to " +
                                    "teleport to its home location when it's low on health.");
                        } else {
                            sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material");
                        }
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "/guarddogs materials [create|disable|ignore] [Material] " +
                                ChatColor.GOLD + "- set the material required to perform the specified guard dog action");
                        sender.sendMessage(ChatColor.RED + "/guarddogs materials [extradamage|ignitechance|teleport] " +
                                "[Material]" + ChatColor.GOLD + "- set the material required for the specified guard " +
                                "dog special ability");
                        break;
                }
                return true;
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("max")) {
                switch (args[1]) {
                    case "extradamage":
                        extraDamageMax = Integer.parseInt(args[2]);
                        sender.sendMessage(ChatColor.GREEN + "Guard Dogs on this server will from now on be able to " +
                                "deal up to " + args[2] + " extra damage. Existing higher values have not been reset.");
                        break;
                    case "ignitechance":
                        if (Integer.parseInt(args[2]) <= 10) {
                            igniteChanceMax = Integer.parseInt(args[2]);
                            sender.sendMessage(ChatColor.GREEN + "Guard Dogs on this server will from now on be able to " +
                                    "have a maximum chance of " + args[2] + "0% to set their enemies on fire. Existing " +
                                    "higher values have not been reset.");
                        } else {
                            sender.sendMessage(ChatColor.DARK_AQUA  + "ignitechance" + ChatColor.RED + " is counted " +
                                    "in setps of 10%, so 10 (100%) is maximum.");
                        }
                        break;
                    case "teleport":
                        teleportMax = Integer.parseInt(args[2]);
                        sender.sendMessage(ChatColor.GREEN + "Guard Dogs on this server will from now on be able to " +
                                "teleport home when lowon health for up to " + args[2] + " times. Existing higher " +
                                "values have not been reset.");
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "/guarddogs max [extradamage|ignitechance|teleport] " +
                                "[maximum]" + ChatColor.GOLD + " - set the maximum for the specified guard dog action.");
                        sender.sendMessage(ChatColor.DARK_AQUA + "extradamage" + ChatColor.GOLD + " - sets the " +
                                "maximum amount of extra damage a guard dog can deal. Counted if half-hearts.");
                        sender.sendMessage(ChatColor.DARK_AQUA + "ignitechance" + ChatColor.GOLD + " - sets the " +
                                "maximum chance of setting an enemy on fire that a guard dog can have. Counted in " +
                                "10%, maximum is 10 (100%).");
                        sender.sendMessage(ChatColor.DARK_AQUA + "teleport" + ChatColor.GOLD + " - sets the maximum " +
                                "amount of times a guard dog can teleport home when low on health.");
                        break;
                }

                return true;
            }
        }

        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "/guarddogs version" + ChatColor.GOLD + " - displays version " +
                "information");
        sender.sendMessage(ChatColor.RED + "/guarddogs materials [create|disable|ignore] [Material]" +
                ChatColor.GOLD + " - set the material required to perform the specified guard dog action");
        sender.sendMessage(ChatColor.RED + "/guarddogs materials [extradamage|ignitechance|teleport] " +
                "[Material]" + ChatColor.GOLD + " - set the material required for the specified guard " +
                "dog special ability");
        sender.sendMessage(ChatColor.RED + "/guarddogs [enable|disable] notifyupdates" + ChatColor.GOLD +
                " - enables or disables the on-join update alert for admins");
        sender.sendMessage(ChatColor.RED + "/guarddogs [enable|disable] [extradamage|ignitechance|teleport]" +
                ChatColor.GOLD + " - enables or disables the specified guard dog special ability");
        sender.sendMessage(ChatColor.RED + "/guarddogs max [extradamage|ignitechance|teleport] [maximum]" +
                ChatColor.GOLD + " - sets the maximum for the specified guard dog special ability.");
    }
}
