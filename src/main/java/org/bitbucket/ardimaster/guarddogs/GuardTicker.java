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

import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Timer handling guard dog cooldowns and resetting stray guard dogs.
 * @author ArdiMaster
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
                    plugin.guardWaits.put(wolf, (plugin.guardWaits.get(wolf) - 10));
                }
            }

            if (plugin.guardTargets.containsKey(wolf) || wolf.isSitting()) {
                continue;
            }

            plugin.guardWaits.put(wolf, 20);
            wolf.teleport(plugin.guardPositions.get(wolf));
            wolf.setSitting(true);
        }
    }
}
