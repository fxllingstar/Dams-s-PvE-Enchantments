package me.st4r.DPE.stages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class Stage4 {
    private static BukkitTask rageTask;
    private static BukkitTask collapseTicker; 

    public static void startRageMode(JavaPlugin plugin, EnderDragon dragon) {

        stopRageMode();


        var speedAttr = dragon.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speedAttr.getBaseValue() * 1.8); 
        }


        rageTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (dragon.isDead() || !dragon.isValid()) {
                    cancel();
                    rageTask = null;
                    return;
                }

                var players = dragon.getWorld().getPlayers();
                if (!players.isEmpty()) {
                    Player target = players.get((int) (Math.random() * players.size()));
                    dragon.setTarget(target);
                    dragon.setPhase(EnderDragon.Phase.CHARGE_PLAYER);
                }
            }
        }.runTaskTimer(plugin, 0L, 200L);


        collapseTicker = new BukkitRunnable() {
            @Override
            public void run() {
                if (dragon.isDead() || !dragon.isValid()) {
                    cancel();
                    collapseTicker = null;
                    return;
                }
                
       
                Location arenaCenter = new Location(dragon.getWorld(), 0, 64, 0);
                triggerVoidCollapse(plugin, arenaCenter);
            }
        }.runTaskTimer(plugin, 0L, 600L); 
    }

    public static void stopRageMode() {
        if (rageTask != null) {
            rageTask.cancel();
            rageTask = null;
        }
        if (collapseTicker != null) {
            collapseTicker.cancel();
            collapseTicker = null;
        }
    }

    private static void triggerVoidCollapse(JavaPlugin plugin, Location arenaCenter) {
        var world = arenaCenter.getWorld();
        

        for (Player p : world.getPlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 160, 0));
            p.sendMessage(Component.text("VOID COLLAPSE INCOMING! HIDE BEHIND AN OBSIDIAN PILLAR!", NamedTextColor.RED, TextDecoration.BOLD));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }


        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : world.getPlayers()) {
                if (isProtectedByPillar(arenaCenter, player.getLocation())) {
                    player.sendMessage(Component.text("The Obsidian Pillar shielded you from the collapse.", NamedTextColor.GREEN));
                } else {
    
                    applyTrueDamage(player, 24.0);
                    
                    player.setVelocity(new Vector(0, 1.5, 0)); 
                    player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation(), 3);
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.5f);
                }
            }
        }, 100L);
    }

    private static boolean isProtectedByPillar(Location source, Location playerLoc) {
        Location start = source.clone();
        Location end = playerLoc.clone();
        

        start.setY(end.getY() + 1.0);
        end.setY(end.getY() + 1.0);

        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        
        if (distance == 0) return false;
        direction.normalize();

        for (double d = 1.0; d < distance; d += 0.5) {
            Location check = start.clone().add(direction.clone().multiply(d));
            if (check.getBlock().getType() == Material.OBSIDIAN) {
                return true;
            }
        }
        return false;
    }

    private static void applyTrueDamage(Player player, double amount) {
        double absorption = player.getAbsorptionAmount();
        
        if (absorption > 0) {
            if (absorption >= amount) {
                player.setAbsorptionAmount(absorption - amount);
                amount = 0;
            } else {
                amount -= absorption;
                player.setAbsorptionAmount(0);
            }
        }

   
        if (amount > 0) {
            double newHealth = player.getHealth() - amount;
            if (newHealth <= 0) {
                player.damage(24.0); 
            } else {
                player.setHealth(newHealth);
                player.damage(0.01); 
            }
        }
    }
}