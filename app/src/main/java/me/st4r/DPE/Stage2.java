package me.st4r.DPE;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class Stage2 {
    
    public static void triggerVoidCracks(JavaPlugin plugin, EnderDragon dragon) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (dragon.isDead() || !dragon.isValid()) {
                    cancel();
                    return;
                }

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

                                crackCenter.getWorld().spawn(crackCenter, org.bukkit.entity.AreaEffectCloud.class, cloud -> {
                                    cloud.setParticle(Particle.DRAGON_BREATH);
                                    cloud.setRadius(3.0f);
                                    cloud.setDuration(100);
                                });
                            }
                        }
                    }, 20L);
                }
            }
        }.runTaskTimer(plugin, 0L, 200L);
    }
}
