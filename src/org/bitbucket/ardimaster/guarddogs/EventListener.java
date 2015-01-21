package org.bitbucket.ardimaster.guarddogs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by ArdiMaster on 19.01.15.
 */
public class EventListener implements Listener {
    protected GuardDogs plugin;

    public EventListener(GuardDogs plugin) {
        this.plugin =   plugin;
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
                    wolf.setSitting(true);
                    plugin.guardTargets.remove(wolf);
                    wolf.teleport(plugin.guardPositions.get(wolf));
                }
            }
        }
    }

    // TODO: Move to syncRepeatedTask running every 5 Ticks and extend.
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        LivingEntity playerEntity = player;
        ArrayList<LivingEntity> near = new ArrayList<>();
        Random rand = new Random();
        double radius = 15d;

        for (Wolf wolf : plugin.guards) {
            if (!wolf.isSitting()) {
                continue;
            }

            List<LivingEntity> all = wolf.getLocation().getWorld().getLivingEntities();

            for (LivingEntity e : all) {
                if (e.getLocation().distance(wolf.getLocation()) <= radius) {
                    if (wolf.getOwner().equals(player) && e.equals(playerEntity)) {
                        continue;
                    }

                    if (e instanceof Wolf) {
                        if (plugin.guards.contains(e)) {
                            continue;
                        }
                    }
                    near.add(e);
                }
            }

            LivingEntity target = near.get(rand.nextInt(near.size()));
            plugin.guardTargets.put(wolf, target);
            wolf.setSitting(false);
            wolf.damage(0d, target);
        }
    }
}
