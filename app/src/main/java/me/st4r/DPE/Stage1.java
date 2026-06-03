package me.st4r.DPE;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

public class Stage1 {
    private static long lastCrystalLaserTime = 0;

    public static boolean attemptCrystalLaser(JavaPlugin plugin, Location crystalLoc, Player player) {
        long now = System.currentTimeMillis();
        if (now - lastCrystalLaserTime < 30000) return false; 

        lastCrystalLaserTime = now;
        Location spawnLoc = crystalLoc.clone().add(0, -0.5, 0);

        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 0.5f);
        player.playSound(crystalLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            crystalLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, spawnLoc, 5, 0.2, 0.2, 0.2, 0.02);
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
                guardian.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10, false, false));
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