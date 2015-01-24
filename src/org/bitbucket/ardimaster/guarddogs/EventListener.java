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
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;

/**
 * Created by ArdiMaster on 19.01.15.
 */
public class EventListener implements Listener {
    protected GuardDogs plugin;

    public EventListener(GuardDogs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Wolf)) {
            return;
        }

        Wolf wolf = (Wolf) event.getRightClicked();
        Player player = event.getPlayer();

        if (player.getItemInHand().getType().equals(Material.PUMPKIN_SEEDS)) {
            if (!wolf.isTamed()) {
                player.sendMessage(ChatColor.RED + "You can't make this dog your guard dog as it isn't tamed!");
                return;
            }

            if (!wolf.getOwner().equals(player)) {
                player.sendMessage(ChatColor.RED + "You can't make this dog your guard dog as it isn't yours!");
                return;
            }

            if (plugin.createGuard(wolf)) {
                player.getInventory().removeItem(new ItemStack(Material.PUMPKIN_SEEDS, 1));
                player.sendMessage(ChatColor.DARK_GREEN + "Guard dog" + ChatColor.GREEN + " ready for action");
                plugin.guardPositions.put(wolf, wolf.getLocation());
                plugin.guardWaits.put(wolf, 40);
                wolf.setSitting(true);
            } else {
                player.sendMessage(ChatColor.RED + "This is already your guard dog!");
            }

        } else if (player.getItemInHand().getType().equals(Material.STICK)) {
            if (!wolf.isTamed() || !wolf.getOwner().equals(player)) {
                player.sendMessage(ChatColor.RED + "This isn't your dog. Thus, it can't be your guard dog. " +
                        "Thus, you can't disable it.");
                return;
            }

            if (plugin.removeGuard(wolf, player)) {
                plugin.guardPositions.remove(wolf);
                player.sendMessage(ChatColor.DARK_GREEN + "Guard dog " + ChatColor.AQUA + "disabled.");
            } else {
                player.sendMessage(ChatColor.RED + "This isn't a guard dog, it's just a normal dog!");
            }
        } else if (player.getItemInHand().getType().equals(Material.GOLD_NUGGET)) {
            if (!wolf.isTamed() || !wolf.getOwner().equals(player)) {
                player.sendMessage(ChatColor.RED + "This isn't your dog. Thus, it can't be your guard dog. " +
                        "Thus, you can't set it's ignores.");
                return;
            }

            if (!plugin.guards.contains(wolf)) {
                player.sendMessage(ChatColor.RED + "This isn't a guard dog. Thus, you can't set it's ignores.");
                return;
            }

            if (plugin.settingIgnore.containsKey(player)) {
                player.sendMessage(ChatColor.RED + "You already started setting an ignore for another guard dog, " +
                        "cancelling old process...");
            }

            player.sendMessage(ChatColor.MAGIC + "M" + ChatColor.RESET + ChatColor.DARK_AQUA + "Type the name of the " +
                    "player you wish to have this guard dog ignore.");

            plugin.settingIgnore.put(player, wolf);
        }

    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (plugin.targetDetermination) {
            return;
        }

        if (event.getEntity() instanceof Wolf) {
            Entity e = event.getEntity();
            if (plugin.guards.contains(e)) {
                if (!plugin.guardTargets.containsKey(e)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event){
        if (event.getEntity() instanceof Wolf) {
            Wolf wolf = (Wolf) event.getEntity();
            plugin.deadGuard(wolf);
            plugin.guardPositions.remove(wolf);
        }

        LivingEntity deadEntity = event.getEntity();
        if (plugin.guardTargets.containsValue(deadEntity)) {
            for (Wolf wolf : plugin.guards) {
                if (plugin.guardTargets.get(wolf).equals(deadEntity)) {
                    plugin.guardWaits.put(wolf, 5 * 20);
                    wolf.setSitting(true);
                    plugin.guardTargets.remove(wolf);
                    wolf.teleport(plugin.guardPositions.get(wolf));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.settingIgnore.containsKey(event.getPlayer())) {
            return;
        }

        Player player = event.getPlayer();
        Wolf wolf = plugin.settingIgnore.get(player);
        if (!plugin.getServer().getPlayer(event.getMessage()).isOnline()) {
            player.sendMessage(ChatColor.RED + "This player is not online.");
            return;
        }
        if (plugin.guardIgnores.containsKey(wolf)) {
            HashSet<String> ignores = plugin.guardIgnores.get(wolf);
            ignores.add(event.getMessage());
            plugin.guardIgnores.put(wolf, ignores);
        } else {
            HashSet<String> ignores = new HashSet<>();
            ignores.add(event.getMessage());
            plugin.guardIgnores.put(wolf, ignores);
        }
        event.setCancelled(true);
        player.sendMessage(ChatColor.DARK_GREEN + event.getMessage() + ChatColor.GREEN + " successfully added.");
    }
}
