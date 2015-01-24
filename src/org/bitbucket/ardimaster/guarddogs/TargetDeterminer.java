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

import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by ArdiMaster on 21.01.15.
 */
public class TargetDeterminer extends BukkitRunnable {
    private GuardDogs plugin;

    public TargetDeterminer(GuardDogs plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.targetDetermination = true;

        Random rand = new Random();
        double radiusSquare = 15 * 15;

        for (Wolf wolf : plugin.guards) {
            if (!wolf.isSitting() || plugin.guardWaits.containsKey(wolf)) {
                continue;
            }

            List<LivingEntity> all = wolf.getLocation().getWorld().getLivingEntities();
            ArrayList<LivingEntity> near = new ArrayList<>();
            ArrayList<Player> nearPlayers = new ArrayList<>();

            for (LivingEntity e : all) {
                if (e.getLocation().distanceSquared(wolf.getLocation()) <= radiusSquare) {
                    if (wolf.getOwner().equals(e)) {
                        continue;
                    }

                    if (e instanceof Wolf) {
                        if (plugin.guards.contains(e)) {
                            continue;
                        }
                        if (wolf.getOwner().equals(((Wolf) e).getOwner())) {
                            continue;
                        }
                    }

                    int yWolf = wolf.getLocation().getBlockY();
                    int yE = e.getLocation().getBlockY();
                    int yDelta = yE - yWolf;
                    if (yDelta > -6 && yDelta < 6) {
                        if (e instanceof Player) {
                            if (plugin.guardIgnores.containsKey(wolf)) {
                                String p = ((Player) e).getName();
                                if (plugin.guardIgnores.get(wolf).contains(p)) {
                                    continue;
                                }
                            }
                            nearPlayers.add((Player) e);
                        } else {
                            if (!(e instanceof Sheep) && !(e instanceof Chicken) && !(e instanceof Cow) &&
                                    !(e instanceof Pig) && !(e instanceof Horse)) {
                                near.add(e);
                            }
                        }
                    }
                }
            }

            LivingEntity target;
            if (!nearPlayers.isEmpty()) {
                target = nearPlayers.get(rand.nextInt(nearPlayers.size()));
            } else if (!near.isEmpty()) {
                target = near.get(rand.nextInt(near.size()));
            } else {
                continue;
            }
            plugin.guardTargets.put(wolf, target);
            wolf.setSitting(false);
            wolf.damage(0, target);
        }

        plugin.targetDetermination = false;
    }
}
