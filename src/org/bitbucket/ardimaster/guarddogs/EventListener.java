package org.bitbucket.ardimaster.guarddogs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Created by ArdiMaster on 19.01.15.
 */
public class EventListener implements Listener {
    protected GuardDogs plugin;

    public EventListener(GuardDogs plugin){
        this.plugin =   plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event){
        if(!(event.getRightClicked() instanceof Wolf))
            return;

        Wolf wolf       =   (Wolf) event.getRightClicked();
        Player player   =   event.getPlayer();

        if(!wolf.isTamed() || !wolf.getOwner().equals(player))
            return;

        if(!player.getItemInHand().getType().equals(Material.PUMPKIN_SEEDS))
            return;

        if(!plugin.createGuard(wolf))
            return;

        player.getInventory().removeItem(new ItemStack(Material.PUMPKIN_SEEDS,1));
        player.sendMessage(ChatColor.GREEN+"Guard Dog"+ChatColor.WHITE+" ready for action");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event){
        if(!(event.getEntity() instanceof Wolf))
            return;

        Wolf wolf   =   (Wolf) event.getEntity();
        plugin.destroyGuard(wolf);
    }

    /*
    TODO: Create a listener and have guard dogs attack nearby players. IDK how to do this yet.
    */
}
