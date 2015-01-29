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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by ArdiMaster on 19.01.15.
 */
public class GuardDogs extends JavaPlugin {

    protected String configFileName = "config.yml";
    protected HashSet<Wolf> guards = new HashSet<>();
    protected HashMap<Wolf, LivingEntity> guardTargets = new HashMap<>();
    protected HashMap<Wolf, Location> guardPositions = new HashMap<>();
    protected HashMap<Wolf, Integer> guardWaits = new HashMap<>();
    protected HashMap<Wolf, HashSet<String>> guardIgnores = new HashMap<>();
    protected HashMap<Player, Wolf> settingIgnore = new HashMap<>();
    protected boolean targetDetermination = false;
    protected Material createMat, disableMat, ignoreMat = null;
    private BukkitTask targetDeterminer;
    private BukkitTask guardTicker;

    @Override
    public void onEnable() {
        loadGuards();
        targetDeterminer = new TargetDeterminer(this).runTaskTimer(this, 30 * 20, 10);
        guardTicker = new GuardTicker(this).runTaskTimer(this, 15 * 20, 10);
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
    }

    @Override
    public void onDisable() {
        targetDeterminer.cancel();
        guardTicker.cancel();
        saveGuards();
    }

    public void logMessage(String message) {
        getLogger().info("[" + getDescription().getName() + " " + getDescription().getVersion() + "] " + message);
    }

    public boolean createGuard(Wolf wolf) {
        if (guards.contains(wolf)) {
            return false;
        }
        guards.add(wolf);
        wolf.setCollarColor(DyeColor.LIME);
        wolf.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 1));
        logMessage("A new guard dog has been created: " + wolf.getUniqueId().toString());
        saveGuards();
        return true;
    }

    public void deadGuard(Wolf wolf) {
        if (!guards.contains(wolf)) {
            return;
        }

        guards.remove(wolf);
        logMessage("A guard dog has died: " + wolf.getUniqueId().toString());
        saveGuards();
        if (wolf.getOwner() instanceof Player) {
            Player player = (Player) wolf.getOwner();
            if (player.isOnline()) {
                player.sendMessage(ChatColor.RED + "One of your " + ChatColor.DARK_GREEN + "Guard Dogs" +
                        ChatColor.RED + " has died.");
            }
        }
    }

    public boolean removeGuard(Wolf wolf, Player player) {
        if (!guards.contains(wolf)) {
            return false;
        }

        guards.remove(wolf);
        logMessage("A guard dog has been removed: " + wolf.getUniqueId().toString());
        saveGuards();
        return true;
    }

    public void saveGuards() {
        File configFile = new File(getDataFolder(), configFileName);
        if (configFile.exists()) {
            configFile.delete();
        }

        /* if (guards.isEmpty()) {
            return;
        } */

        try {
            if (!Files.exists(getDataFolder().toPath())) {
                Files.createDirectory(getDataFolder().toPath());
            }
            configFile.createNewFile();
        } catch (IOException e) {
            logMessage("Unable to create config file!");
            e.printStackTrace();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        List<String> guardIds = new ArrayList<>();

        for (Wolf wolf : guards) {
            String id = wolf.getUniqueId().toString();
            Location loc = guardPositions.get(wolf);
            guardIds.add(id);
            config.set("guards." + id + ".world", loc.getWorld().getName());
            config.set("guards." + id + ".X", loc.getBlockX());
            config.set("guards." + id + ".Y", loc.getBlockY());
            config.set("guards." + id + ".Z", loc.getBlockZ());

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

        config.set("guards.guardids", guardIds);
        config.set("version", getDescription().getVersion());
        try {
            config.save(configFile);
        } catch (IOException e) {
            logMessage("Unable to save guard file!");
        }

    }

    protected void loadGuards() {
        boolean newConfig = true;
        File configFile = new File(getDataFolder(), configFileName);
        if (!configFile.exists()) {
            logMessage("No config file.");
            createMat = Material.PUMPKIN_SEEDS;
            disableMat = Material.STICK;
            ignoreMat = Material.GOLD_NUGGET;
            if (Files.exists(new File(getDataFolder(), "guards.yml").toPath())) {
                logMessage("Found old guards.yml, renaming...");
                try {
                    Files.move(new File(getDataFolder(), "guards.yml").toPath(), new File(getDataFolder(),
                            configFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logMessage("Succeeded, proceeding to load old configuration scheme (will be converted on " +
                            "next save)");
                    newConfig = false;
                } catch (IOException e) {
                    logMessage("Unable to rename guards.yml to config.yml, proceeding with empty config.");
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

        for (World world : getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Wolf) {
                    if (guardIds.contains(entity.getUniqueId().toString())) {
                        Wolf wolf = (Wolf) entity;
                        createGuard(wolf);
                        guardWaits.put(wolf, 40);
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
                        guardPositions.put(wolf, pos);
                        wolf.setSitting(true);

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

        if (newConfig) {
            createMat = Material.getMaterial(config.getString("id.create"));
            disableMat = Material.getMaterial(config.getString("id.disable"));
            ignoreMat = Material.getMaterial(config.getString("id.ignore"));
        }
    }

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
