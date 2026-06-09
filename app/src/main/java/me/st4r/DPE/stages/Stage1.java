package me.st4r.DPE.stages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Stage1 {
    private static long lastCrystalLaserTime = 0;
    private static long lastDragonScreechTime = 0;
    private static BukkitTask stage1Ticker = null;
    private static final long CRYSTAL_LASER_COOLDOWN_MS = 25000L;
    private static final long DRAGON_SCREECH_COOLDOWN_MS = 12000L;

    public static void startStageTasks(JavaPlugin plugin, EnderDragon dragon) {
        Bukkit.getLogger().info("[Debug] Entering startStageTasks");
        stopStageTasks();
        resetCooldowns();
        stage1Ticker = new BukkitRunnable() { // Fixed typo here
            int ticks = 0;
            @Override
            public void run() {
                // Fixed: Added debug visibility inside the ticker loop if needed, but keeping it silent unless ticking
                if (dragon.isDead() || !dragon.isValid()) {
                    Bukkit.getLogger().info("[Debug] Stage1 Ticker cancelled - Dragon dead/invalid");
                    cancel();
                    return;
                }
                ticks += 20;

                if (ticks % 300 == 0) {
                    triggerDragonScreech(dragon);
                }

                if (ticks % 100 == 0) {
                    attemptPeriodicLaser(plugin, dragon);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public static void stopStageTasks() {
        Bukkit.getLogger().info("[Debug] Entering stopStageTasks");
        if (stage1Ticker != null) {
            stage1Ticker.cancel();
            stage1Ticker = null;
        }
    }

    private static void resetCooldowns() {
        Bukkit.getLogger().info("[Debug] Entering resetCooldowns");
        lastCrystalLaserTime = 0L;
        lastDragonScreechTime = 0L;
    }

    private static void triggerDragonScreech(EnderDragon dragon) {
        Bukkit.getLogger().info("[Debug] Entering triggerDragonScreech");
        long now = System.currentTimeMillis();
        if (now - lastDragonScreechTime < DRAGON_SCREECH_COOLDOWN_MS) {
            return;
        }
        lastDragonScreechTime = now;

        for (Player player : dragon.getWorld().getPlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.5f);
            player.sendHurtAnimation(0.0f); 
            player.sendMessage(Component.text("The Dragon lets out a Deafening Screech!", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        }
    }

    private static void attemptPeriodicLaser(JavaPlugin plugin, EnderDragon dragon) {
        Bukkit.getLogger().info("[Debug] Entering attemptPeriodicLaser");
        List<EnderCrystal> crystals = new ArrayList<>();
        dragon.getWorld().getEntitiesByClass(EnderCrystal.class).forEach(crystals::add);
        if (crystals.isEmpty()) return;

        List<Player> players = dragon.getWorld().getPlayers();
        if (players.isEmpty()) return;

        EnderCrystal crystal = crystals.get((int) (Math.random() * crystals.size()));
        Player target = players.get((int) (Math.random() * players.size()));
        attemptCrystalLaser(plugin, crystal.getLocation(), target);
    }

    public static boolean attemptCrystalLaser(JavaPlugin plugin, Location crystalLoc, Player player) {
        Bukkit.getLogger().info("[Debug] Entering attemptCrystalLaser for " + player.getName());
        long now = System.currentTimeMillis();
        if (now - lastCrystalLaserTime < CRYSTAL_LASER_COOLDOWN_MS) return false; 

        lastCrystalLaserTime = now;
        Location spawnLoc = crystalLoc.clone().add(0, 0.5, 0);

        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 0.5f);
        player.playSound(crystalLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);

        int telegraphTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0.1);
            crystalLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, spawnLoc, 4, 0.1, 0.1, 0.1, 0.02);
        }, 0L, 2L).getTaskId(); 

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getLogger().info("[Debug] Executing Laser Hit Task");
            Bukkit.getScheduler().cancelTask(telegraphTaskId);

            if (!player.isOnline() || !player.getWorld().equals(crystalLoc.getWorld())) return;

            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.2f);

            new BukkitRunnable() {
                int runTicks = 0;

                @Override
                public void run() {
                    if (runTicks > 15 || !player.isOnline() || !player.getWorld().equals(crystalLoc.getWorld())) {
                        if (player.isOnline()) {
                            Bukkit.getLogger().info("[Debug] Applying laser damage to " + player.getName());
                            player.damage(8.0);
                            player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 1);
                        }
                        cancel();
                        return;
                    }

                    Location targetPoint = player.getLocation().add(0, 1.2, 0);
                    Vector originVec = spawnLoc.toVector();
                    Vector targetVec = targetPoint.toVector();
                    Vector direction = targetVec.subtract(originVec);
                    double distance = direction.length();
                    direction.normalize();

                    for (double d = 0; d < distance; d += 0.25) {
                        Location point = spawnLoc.clone().add(direction.clone().multiply(d));
                        point.getWorld().spawnParticle(Particle.ENCHANTED_HIT, point, 1, 0, 0, 0, 0);
                    }
                    runTicks += 2;
                }
            }.runTaskTimer(plugin, 0L, 2L);

        }, 40L); 

        return true;
    }
}