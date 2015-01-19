package org.bitbucket.ardimaster.guarddogs;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ArdiMaster on 19.01.15.
 */
public class GuardDogs extends JavaPlugin {

    protected String guardFileName = "guards";
    protected HashSet<Wolf> guards = new HashSet<>();

    @Override
    public void onEnable() {
        loadGuards();
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
    }

    @Override
    public void onDisable() {
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
        wolf.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,300,1));
        logMessage("A new guard dog has been created: " + wolf.getUniqueId().toString());
        saveGuards();
        return true;
    }

    public void destroyGuard(Wolf wolf) {
        if (!guards.contains(wolf)) {
            return;
        }

        guards.remove(wolf);
        logMessage("A guard dog has been destroyed: " + wolf.getUniqueId().toString());
        saveGuards();
        if ((wolf.getOwner() instanceof Player)) {
            Player player = (Player) wolf.getOwner();
            if (player.isOnline()) {
                player.sendMessage(ChatColor.WHITE + "One of your " + ChatColor.GREEN + "Guard Dogs" +
                        ChatColor.WHITE + " has died.");
            }
        }
    }

    public void saveGuards() {
        File guardFile  =   new File(getDataFolder(),guardFileName);
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
        Iterator<Wolf> wolfIterator = guards.iterator();
        List<String> guardIds = new ArrayList<>();
        while (wolfIterator.hasNext()) {
            Wolf wolf = wolfIterator.next();
            guardIds.add(wolf.getUniqueId().toString());

        }
        config.set("guards",guardIds);
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

        Iterator<World> worldIterator = getServer().getWorlds().iterator();
        while (worldIterator.hasNext()) { // All worlds on server
            World world = worldIterator.next();

            Iterator<LivingEntity> entityIterator = world.getLivingEntities().iterator();
            while (entityIterator.hasNext()) { // All living entities in world
                LivingEntity entity = entityIterator.next();

                if(entity instanceof Wolf){
                    if (guardIds.contains(entity.getUniqueId().toString())) {
                        createGuard((Wolf) entity);
                    }
                }
            }
        }

    }
}
