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

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by ArdiMaster on 19.01.15.
 */
public class GuardDogs extends JavaPlugin {

    protected String guardFileName = "guards.yml";
    protected HashSet<Wolf> guards = new HashSet<>();
    protected HashMap<Wolf, LivingEntity> guardTargets = new HashMap<>();
    protected HashMap<Wolf, Location> guardPositions = new HashMap<>();
    protected HashMap<Wolf, Integer> guardWaits = new HashMap<>();
    protected HashMap<Wolf, HashSet<String>> guardIgnores = new HashMap<>();
    protected HashMap<Player, Wolf> settingIgnore = new HashMap<>();
    protected boolean targetDetermination = false;
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
        player.sendMessage(ChatColor.WHITE + "One of your " + ChatColor.GREEN + "Guard Dogs" +
                ChatColor.WHITE + " has died.");
        return true;
    }

    public void saveGuards() {
        File guardFile = new File(getDataFolder(), guardFileName);
        if (guardFile.exists()) {
            guardFile.delete();
        }
        if (guards.isEmpty()) {
            return;
        }
        try {
            guardFile.createNewFile();
        } catch (IOException e) {
            logMessage("Unable to create guard file!");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(guardFile);
        List<String> guardIds = new ArrayList<>();

        for (Wolf wolf : guards) {
            String id = wolf.getUniqueId().toString();
            Location loc = guardPositions.get(wolf);
            guardIds.add(id);
            config.set(id + ".world", loc.getWorld().getName());
            config.set(id + ".X", loc.getBlockX());
            config.set(id + ".Y", loc.getBlockY());
            config.set(id + ".Z", loc.getBlockZ());

            if (guardIgnores.containsKey(wolf)) {
                ArrayList<String> ignores = new ArrayList<>();
                for (String s : guardIgnores.get(wolf)) {
                    ignores.add(s);
                }
                config.set(id + ".ignores", ignores);
            }
        }

        config.set("guards", guardIds);
        try {
            config.save(guardFile);
        } catch (IOException e) {
            logMessage("Unable to save guard file!");
        }

    }

    protected void loadGuards() {
        File guardFile  =   new File(getDataFolder(),guardFileName);
        if (!guardFile.exists()) {
            logMessage("No guard file.");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(guardFile);
        List<String> guardIds = config.getStringList("guards");

        for (World world : getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Wolf) {
                    if (guardIds.contains(entity.getUniqueId().toString())) {
                        Wolf wolf = (Wolf) entity;
                        createGuard(wolf);
                        guardWaits.put(wolf, 40);
                        String uuid = entity.getUniqueId().toString();
                        World posWorld = getServer().getWorld((String) config.get(uuid + ".world"));
                        int X = Integer.parseInt((String) config.get(uuid + ".X"));
                        int Y = Integer.parseInt((String) config.get(uuid + ".Y"));
                        int Z = Integer.parseInt((String) config.get(uuid + ".Z"));
                        Location pos = new Location(posWorld, X, Y, Z);
                        entity.teleport(pos);
                        guardPositions.put(wolf, pos);
                        ((Wolf) entity).setSitting(true);
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
}
