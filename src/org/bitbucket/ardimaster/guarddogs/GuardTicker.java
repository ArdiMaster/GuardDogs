package org.bitbucket.ardimaster.guarddogs;

import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Created by adrianwelcker on 23.01.15.
 */
public class GuardTicker extends BukkitRunnable {
    private GuardDogs plugin;

    public GuardTicker(GuardDogs plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Wolf wolf : plugin.guards) {
            if (plugin.guardWaits.containsKey(wolf)) {
                if (plugin.guardWaits.get(wolf) <= 0) {
                    plugin.guardWaits.remove(wolf);
                } else {
                    plugin.guardWaits.put(wolf, (plugin.guardWaits.get(wolf) - 5));
                }
            }
        }
    }
}
