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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Random;

/**
 * The Bukkit event listening class for the GuardDogs plugin
 */
public class EventListener implements Listener {
    protected GuardDogs plugin;
    private Random random = new Random();

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

        if (player.getItemInHand().getType().equals(plugin.createMat)) {
            if (!wolf.isTamed()) {
                player.sendMessage(ChatColor.RED + "You can't make this dog your guard dog as it isn't tamed!");
                return;
            }

            if (!wolf.getOwner().equals(player)) {
                player.sendMessage(ChatColor.RED + "You can't make this dog your guard dog as it isn't yours!");
                return;
            }

            if (plugin.createGuard(wolf, 0, 0, 0)) {
                player.getInventory().removeItem(new ItemStack(plugin.createMat, 1));
                player.sendMessage(ChatColor.DARK_GREEN + "Guard dog" + ChatColor.GREEN + " ready for action");
            } else {
                player.sendMessage(ChatColor.RED + "This is already your guard dog!");
            }

        } else if (player.getItemInHand().getType().equals(plugin.disableMat)) {
            if (!wolf.isTamed() || !wolf.getOwner().equals(player)) {
                player.sendMessage(ChatColor.RED + "This isn't your dog. Thus, it can't be your guard dog. " +
                        "Thus, you can't disable it.");
                return;
            }

            if (plugin.removeGuard(wolf)) {
                player.sendMessage(ChatColor.DARK_GREEN + "Guard dog " + ChatColor.AQUA + "disabled.");
            } else {
                player.sendMessage(ChatColor.RED + "This isn't a guard dog, it's just a normal dog!");
            }
        } else if (player.getItemInHand().getType().equals(plugin.ignoreMat)) {
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

            player.sendMessage(ChatColor.MAGIC + "M" + ChatColor.RESET + ChatColor.DARK_AQUA + " Type the name of the " +
                    "player you wish to have this guard dog ignore.");

            plugin.settingIgnore.put(player, wolf);
        } else if (player.getItemInHand().getType().equals(plugin.extraDamageMat)) {
            if (!wolf.isTamed() || !wolf.getOwner().equals(player)) {
                player.sendMessage(ChatColor.RED + "This isn't your dog. Thus, it can't be your guard dog. " +
                        "Thus, you can't have it deal extra damage.");
                return;
            }

            if (!plugin.guards.contains(wolf)) {
                player.sendMessage(ChatColor.RED + "This isn't a guard dog. Thus, you can't have it " +
                        "deal extra damage.");
                return;
            }

            if (!plugin.extraDamage) {
                player.sendMessage(ChatColor.RED + "Guard dogs extra damage is disabled on this server!");
                return;
            }

            int damage = plugin.guardExtraDamage.get(wolf);
            if (damage < 2) {
                player.getInventory().remove(new ItemStack(plugin.extraDamageMat, 1));
                damage++;
                plugin.guardExtraDamage.put(wolf, damage);
                player.sendMessage(ChatColor.GREEN + "This " + ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN +
                        "'s extra damage is now " + ChatColor.AQUA + damage + ChatColor.GREEN + " half-hearts");
            } else {
                // TODO: Make maximum amount of extra damage configurable
                player.sendMessage(ChatColor.RED + "This " + ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.RED +
                        "'s extra damage is already at maximum!");
            }
        } else if (player.getItemInHand().getType().equals(plugin.igniteChanceMat)) {
            if (!wolf.isTamed() || !wolf.getOwner().equals(player)) {
                player.sendMessage(ChatColor.RED + "This isn't your dog. Thus, it can't be your guard dog. " +
                        "Thus, you can't have it ignite it's enemies.");
                return;
            }

            if (!plugin.guards.contains(wolf)) {
                player.sendMessage(ChatColor.RED + "This isn't a guard dog. Thus, you can't have it " +
                        "ignite it's enemies.");
                return;
            }

            if (!plugin.extraDamage) {
                player.sendMessage(ChatColor.RED + "Guard dogs igniting their enemies is disabled on this server!");
                return;
            }

            int chance = plugin.guardIgniteChance.get(wolf);
            if (chance < 6) {
                player.getInventory().remove(new ItemStack(plugin.igniteChanceMat, 1));
                chance++;
                plugin.guardExtraDamage.put(wolf, chance);
                player.sendMessage(ChatColor.GREEN + "This " + ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN +
                        "'s chance to ignite it's enemy is now " + ChatColor.AQUA + chance + ChatColor.GREEN + " %");
            } else {
                // TODO: Make maximum chance configurable
                player.sendMessage(ChatColor.RED + "This " + ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.RED +
                        "'s chance to ignite it's enemies is already at maximum!");
            }
        } else if (player.getItemInHand().getType().equals(plugin.teleportMat)) {
            if (!wolf.isTamed() || !wolf.getOwner().equals(player)) {
                player.sendMessage(ChatColor.RED + "This isn't your dog. Thus, it can't be your guard dog. " +
                        "Thus, you can't have it teleport home when it's low on health.");
                return;
            }

            if (!plugin.guards.contains(wolf)) {
                player.sendMessage(ChatColor.RED + "This isn't a guard dog. Thus, you can't have it " +
                        "teleport home when it's low on health.");
                return;
            }

            if (!plugin.extraDamage) {
                player.sendMessage(ChatColor.RED + "Guard dogs teleporting home when low on health is disabled " +
                        "on this server!");
                return;
            }

            int teleport = plugin.guardTeleportCount.get(wolf);
            if (teleport < 16) {
                player.getInventory().remove(new ItemStack(plugin.teleportMat, 1));
                teleport++;
                plugin.guardExtraDamage.put(wolf, teleport);
                player.sendMessage(ChatColor.GREEN + "This " + ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN +
                        " can now teleport home " + ChatColor.AQUA + teleport + ChatColor.GREEN +
                        " times when low on health.");
            } else {
                // TODO: Make teleport count configurable
                player.sendMessage(ChatColor.RED + "When low on health, this " + ChatColor.DARK_GREEN + "Guard Dog" +
                        ChatColor.RED + " can already teleport home the maximum amount of times!");
            }
        }

    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Wolf) {
            Wolf wolf = (Wolf) event.getDamager();

            if (plugin.guards.contains(wolf)) {
                event.setDamage(event.getDamage() + plugin.guardExtraDamage.get(wolf));

                if (plugin.guardIgniteChance.get(wolf) > 0) {
                    if (random.nextInt(10) < plugin.guardIgniteChance.get(wolf)) {
                        event.getEntity().setFireTicks(20 * 3);
                    }
                }
            }
        }

        if (event.getEntity() instanceof Wolf) {
            Wolf wolf = (Wolf) event.getEntity();

            if (wolf.getHealth() < 6) {
                if (plugin.guardTeleportCount.get(wolf) > 0) {
                    plugin.guardWaits.put(wolf, 12 * 20);
                    wolf.teleport(plugin.guardPositions.get(wolf));
                    wolf.setSitting(true);
                    wolf.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 2));
                    plugin.guardTeleportCount.put(wolf, (plugin.guardTeleportCount.get(wolf) - 1));
                }
            }

            if (plugin.targetDetermination) {
                return;
            }
            if (plugin.guards.contains(wolf) && !plugin.guardTargets.containsKey(wolf)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Wolf) {
            Wolf wolf = (Wolf) event.getEntity();
            plugin.deadGuard(wolf);
        }

        LivingEntity deadEntity = event.getEntity();
        if (plugin.guardTargets.containsValue(deadEntity)) {
            for (Wolf wolf : plugin.guards) {
                if (!plugin.guardTargets.containsKey(wolf)) {
                    continue;
                }

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

        try {
            Player ignore = plugin.getServer().getPlayer(event.getMessage());
        } catch (NullPointerException e) {
            player.sendMessage(ChatColor.RED + "This is not a player, or he/she isn't online!");
            event.setCancelled(true);
            return;
        }

        if (!plugin.getServer().getPlayer(event.getMessage()).isOnline()) {
            player.sendMessage(ChatColor.RED + "This player is not online.");
            event.setCancelled(true);
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.currentVersion.equals("ERROR") ||
                plugin.currentVersion.equals(plugin.getDescription().getVersion()) ||
                !event.getPlayer().hasPermission("guarddogs.admin") ||
                !plugin.notifyUpdates) {
            return;
        }

        event.getPlayer().sendMessage(ChatColor.AQUA + "Version " + ChatColor.GREEN + plugin.currentVersion +
                " of plugin " + ChatColor.DARK_GREEN + "Guard Dogs" + ChatColor.AQUA + " is available! " +
                ChatColor.DARK_AQUA + "Grab it at http://dev.bukkit.org/bukkit-plugins/guard-dogs");
    }
}
