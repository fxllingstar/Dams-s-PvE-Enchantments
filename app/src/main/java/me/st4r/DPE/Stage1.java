package me.st4r.DPE;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class Stage1 {
    private static long lastCrystalLaserTime = 0; 
    private static BukkitTask stage1Ticker = null; 

    public static void startStageTasks(JavaPlugin plugin, EnderDragon dragon) {
        stopStageTasks();
        stage1Ticker = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (dragon.isDead() || !dragon.isValid()) { 
                    cancel(); 
                    return; 
                }
                ticks += 20;

             
                if (ticks % 300 == 0) { 
                    triggerDragonScreech(dragon);
                }

                // Check for overcharge lasers every 5 seconds
                if (ticks % 100 == 0) { 
                    attemptPeriodicLaser(plugin, dragon); 
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); 
    }

    public static void stopStageTasks() {
        if (stage1Ticker != null) { 
            stage1Ticker.cancel(); 
            stage1Ticker = null; 
        }
    }

    private static void triggerDragonScreech(EnderDragon dragon) {
        for (Player player : dragon.getWorld().getPlayers()) { 
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0)); 
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.5f); 
            player.sendHurtAnimation(0.0f); 
            player.sendMessage(Component.text("The Dragon lets out a Deafening Screech!", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        }
    }

    private static void attemptPeriodicLaser(JavaPlugin plugin, EnderDragon dragon) {
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
        long now = System.currentTimeMillis(); 
        if (now - lastCrystalLaserTime < 30000) return false; 

        lastCrystalLaserTime = now;
        Location spawnLoc = crystalLoc.clone().add(0, -0.5, 0); 

        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 0.5f); 
        player.playSound(crystalLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f); 

     
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1); //[cite: 1]
            crystalLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, spawnLoc, 5, 0.2, 0.2, 0.2, 0.02); //[cite: 1]
        }, 0L, 2L).getTaskId();

     
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getScheduler().cancelTask(taskId); 

            if (!player.isOnline() || !player.getWorld().equals(crystalLoc.getWorld())) return;

            Guardian laserPointer = crystalLoc.getWorld().spawn(spawnLoc, Guardian.class, guardian -> {
                guardian.setInvisible(true);
                guardian.setSilent(true); 
                guardian.setInvulnerable(true); 
                guardian.setGravity(false);
                guardian.setAI(false); 
                guardian.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10, false, false)); //[cite: 1]
            });

            laserPointer.setTarget(player); 
            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.2f); 

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.damage(8.0);
                    player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 1); 
                }
                laserPointer.remove(); 
            }, 15L); 
        }, 40L);

        return true; 
    }
}