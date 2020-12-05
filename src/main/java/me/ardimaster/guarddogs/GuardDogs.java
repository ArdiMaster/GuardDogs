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

package me.ardimaster.guarddogs;

import org.bukkit.*;
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
 * The plugin's main class.
 * @author ArdiMaster
 */
public class GuardDogs extends JavaPlugin {
    private final String configFileName = "config.yml";
    HashSet<Wolf> guards = new HashSet<>();
    HashMap<Wolf, LivingEntity> guardTargets = new HashMap<>();
    HashMap<Wolf, Location> guardPositions = new HashMap<>();
    HashMap<Wolf, Integer> guardWaits = new HashMap<>();
    HashMap<Wolf, HashSet<String>> guardIgnores = new HashMap<>();
    HashMap<Player, Wolf> settingIgnore = new HashMap<>();
    HashMap<Wolf, Integer> guardExtraDamage = new HashMap<>();
    HashMap<Wolf, Integer> guardIgniteChance = new HashMap<>();
    HashMap<Wolf, Integer> guardTeleportCount = new HashMap<>();
    boolean targetDetermination = false;
    Material createMat, disableMat, ignoreMat, extraDamageMat, igniteChanceMat, teleportMat = null;
    String currentVersion = "ERROR";
    boolean notifyUpdates, extraDamageEnabled, igniteChanceEnabled, teleportEnabled;
    int extraDamageMax, igniteChanceMax, teleportMax;
    private BukkitTask targetDeterminer;
    private BukkitTask guardTicker;

    @Override
    public void onEnable() {
        loadGuards();
        targetDeterminer = new TargetDeterminer(this).runTaskTimer(this, 30 * 20, 10);
        guardTicker = new GuardTicker(this).runTaskTimer(this, 15 * 20, 10);
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        this.getCommand("guarddogs").setExecutor(new GDCommand(this));

        /* getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                // -- to be rewritten later --
            }
        }); */

        log(Level.INFO, "Plugin loaded and available!");
    }

    /**
     * Handles shutting down the plugin. Cancels timers and saves data.
     */
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
     * This method handles the creation of guard dogs.
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
     * Handles the death of a wolf in the world. If it was a guard dog, it will be removed from all timer checks.
     * @param wolf The wolf which died.
     */
    public void deadGuard(Wolf wolf) {
        if (!guards.contains(wolf)) {
            return;
        }

        disableGuard(wolf);

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

    private void disableGuard(Wolf wolf) {
        guards.remove(wolf);
        guardPositions.remove(wolf);
        guardExtraDamage.remove(wolf);
        guardIgniteChance.remove(wolf);
        guardTeleportCount.remove(wolf);
        guardTargets.remove(wolf);
        guardIgnores.remove(wolf);
        guardWaits.remove(wolf);
    }

    /**
     * Attempts to remove the given wolf from the system.
     * It will no longer function as a guard dog controlled by the plugin.
     * @param wolf The wolf to be removed
     * @return Whether the given wolf was a guard dog (true) or not (false).
     */
    public boolean removeGuard(Wolf wolf) {
        if (!guards.contains(wolf)) {
            return false;
        }

        disableGuard(wolf);

        log(Level.INFO, "A guard dog has been removed: " + wolf.getUniqueId().toString());
        saveGuards();
        return true;
    }

    /**
     * Writes guard dogs and plugin settings to disk.
     */
    public void saveGuards() {
        File configFile = new File(getDataFolder(), configFileName);
        boolean saveGuardsOnly = false;
        if (configFile.exists()) {
            saveGuardsOnly = true;
        } else {
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
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        List<String> guardIds = new ArrayList<>();

        for (Wolf wolf : guards) {
            String id = wolf.getUniqueId().toString();
            Location loc = guardPositions.get(wolf);
            if (loc == null) {
                throw new RuntimeException("Attempting to save a guard dog whose position is null! Something went " +
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
                ArrayList<String> ignores = new ArrayList<>(guardIgnores.get(wolf));
                config.set("guards." + id + ".ignores", ignores);
            }
        }

        if (!saveGuardsOnly) {
            config.set("id.create", createMat.toString());
            config.set("id.disable", disableMat.toString());
            config.set("id.ignore", ignoreMat.toString());
            config.set("id.extraDamage", extraDamageMat.toString());
            config.set("id.igniteChance", igniteChanceMat.toString());
            config.set("id.teleport", teleportMat.toString());
            config.set("special.extraDamage", extraDamageEnabled);
            config.set("special.extraDamageMax", extraDamageMax);
            config.set("special.igniteChance", igniteChanceEnabled);
            config.set("special.igniteChanceMax", igniteChanceMax);
            config.set("special.teleport", teleportEnabled);
            config.set("special.teleportMax", teleportMax);
            config.set("notifyUpdates", notifyUpdates);
        }

        config.set("guards.guardids", guardIds);
        config.set("version", getDescription().getVersion());
        try {
            config.save(configFile);
        } catch (IOException e) {
            log(Level.WARNING, "Unable to save config file!");
        }

    }

    /**
     * Loads guard dogs and plugin settings from disk.
     */
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

                    extraDamageEnabled = true;
                    igniteChanceEnabled = true;
                    teleportEnabled = true;
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

                extraDamageEnabled = true;
                igniteChanceEnabled = true;
                teleportEnabled = true;
                extraDamageMax = 2;
                igniteChanceMax = 6;
                teleportMax = 16;

                notifyUpdates = true;
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (configVersion.equals("unknown")) {
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

                extraDamageEnabled = true;
                igniteChanceEnabled = true;
                teleportEnabled = true;
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

                extraDamageEnabled = true;
                igniteChanceEnabled = true;
                teleportEnabled = true;
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

                extraDamageEnabled = config.getBoolean("special.extraDamage");
                igniteChanceEnabled = config.getBoolean("special.igniteChance");
                teleportEnabled = config.getBoolean("special.teleport");
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
                                    HashSet<String> putIgnores = new HashSet<>(ignores);
                                    guardIgnores.put(wolf, putIgnores);
                                }
                                break;
                            default:
                                if (config.contains("guards." + uuid + ".ignores")) {
                                    List<String> ignores = config.getStringList("guards." + uuid + ".ignores");
                                    HashSet<String> putIgnores = new HashSet<>(ignores);
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
}
