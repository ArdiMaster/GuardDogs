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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

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
}
