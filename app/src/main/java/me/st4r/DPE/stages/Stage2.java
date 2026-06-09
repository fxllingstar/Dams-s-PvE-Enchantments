package me.st4r.DPE.stages;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class Stage2 {
    private static BukkitTask stage2Ticker = null;

    public static void startStage2Mechanics(JavaPlugin plugin, EnderDragon dragon) {
        if (stage2Ticker != null) stage2Ticker.cancel();

        stage2Ticker = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!dragon.isValid() || dragon.isDead()) {
                    cancel();
                    return;
                }
                ticks += 20;

                if (ticks % 200 == 0) {
                    executeVoidCracks(plugin, dragon);
                }

                if (ticks % 300 == 0) {
                    executeTeleportSlam(plugin, dragon);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public static void stopStage2Mechanics() {
        if (stage2Ticker != null) {
            stage2Ticker.cancel();
            stage2Ticker = null;
        }
    }
    
    private static void executeVoidCracks(JavaPlugin plugin, EnderDragon dragon) {
        var world = dragon.getWorld();
        for (Player player : world.getPlayers()) {
            Location crackCenter = player.getLocation().add(
                    ThreadLocalRandom.current().nextDouble(-7.5, 7.5),
                    0,
                    ThreadLocalRandom.current().nextDouble(-7.5, 7.5)
            );
            crackCenter.getWorld().spawnParticle(Particle.PORTAL, crackCenter, 50, 2, 0, 2, 0.1);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!dragon.isValid() || dragon.isDead()) {
                    return;
                }

                for (Player p : world.getPlayers()) {
                    if (!p.getWorld().equals(world)) {
                        continue;
                    }
                    if (p.getLocation().distanceSquared(crackCenter) < 36.0) {
                        Vector pull = crackCenter.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.8);
                        p.setVelocity(pull);

                        crackCenter.getWorld().spawn(crackCenter, AreaEffectCloud.class, cloud -> {
                            cloud.setParticle(Particle.DRAGON_BREATH);
                            cloud.setRadius(3.0f);
                            cloud.setDuration(100);
                        });
                    }
                }
            }, 20L);
        }
    }

    private static void executeTeleportSlam(JavaPlugin plugin, EnderDragon dragon) {
        var players = dragon.getWorld().getPlayers();
        if (players.isEmpty()) return;

        Player victim = players.get(ThreadLocalRandom.current().nextInt(players.size()));
    
        dragon.setPhase(EnderDragon.Phase.LAND_ON_PORTAL);
        
        Location slamHeight = victim.getLocation().add(0, 22, 0);
        slamHeight.setDirection(victim.getLocation().toVector().subtract(slamHeight.toVector()));
        dragon.teleport(slamHeight);
        
        dragon.getWorld().playSound(slamHeight, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.5f);

        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (!dragon.isValid() || dragon.isDead() || elapsed > 40) {
                    if (dragon.isValid() && !dragon.isDead()) {
                        dragon.setVelocity(new Vector(0, 0, 0));
                        dragon.setPhase(EnderDragon.Phase.CIRCLING);
                    }
                    cancel();
                    return;
                }

                Location dragonLoc = dragon.getLocation();
                Location targetLoc = victim.getLocation();
                
                Vector direction = targetLoc.toVector().subtract(dragonLoc.toVector());
                direction.setX(direction.getX() * 0.15);
                direction.setZ(direction.getZ() * 0.15);
                direction.setY(-1.8); 
                dragon.setVelocity(direction);
                
   
                dragon.getWorld().spawnParticle(Particle.DRAGON_BREATH, dragonLoc, 12, 1.5, 0.5, 1.5, 0.15, 1.0f);

                double yDiff = dragonLoc.getY() - targetLoc.getY();
                if (yDiff <= 2.5 && yDiff >= -4.0 && dragonLoc.distanceSquared(targetLoc) < 36.0) { 
                    dragon.getWorld().playSound(dragonLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.5f);
                    dragon.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, dragonLoc, 4);
                    
                    for (Player p : dragon.getWorld().getPlayers()) {
                        if (p.getLocation().distanceSquared(dragonLoc) < 50.0) {
                            p.damage(14.0, dragon);
                            p.setVelocity(p.getLocation().toVector().subtract(dragonLoc.toVector()).normalize().multiply(1.4).setY(0.55));
                        }
                    }
                    
     
                    dragon.setVelocity(new Vector(0, 0.2, 0)); 
                    dragon.setPhase(EnderDragon.Phase.CIRCLING);
                    cancel();
                    return;
                }
                elapsed++;
            }
        }.runTaskTimer(plugin, 2L, 1L);
    }
}