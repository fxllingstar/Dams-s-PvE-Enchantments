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

    public static void startRageMode(JavaPlugin plugin, EnderDragon dragon) {
        if (rageTask != null) {
            rageTask.cancel();
            rageTask = null;
        }

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
    }

    public static void stopRageMode() {
        if (rageTask != null) {
            rageTask.cancel();
            rageTask = null;
        }
    }

    public static void runVoidCollapse(JavaPlugin plugin, Location arenaCenter) {
        var world = arenaCenter.getWorld();
        
        for (Player p : world.getPlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 160, 0));
            p.sendMessage(Component.text("VOID COLLAPSE INCOMING! HIDE BEHIND AN OBSIDIAN PILLAR!", NamedTextColor.RED, TextDecoration.BOLD));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : world.getPlayers()) {
                if (isProtectedByPillar(arenaCenter, player.getLocation())) {
                    player.sendMessage(Component.text("The Obsidian Pillar shielded you from the collapse.", NamedTextColor.GREEN));
                } else {
                    player.damage(18.0);
                    player.setVelocity(new Vector(0, 2.5, 0));
                    player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation(), 3);
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.5f);
                }
            }
        }, 100L);
    }

    private static boolean isProtectedByPillar(Location source, Location playerLoc) {
        Vector direction = playerLoc.toVector().subtract(source.toVector());
        double distance = direction.length();
        direction.normalize();

        for (double d = 1; d < distance; d += 0.5) {
            Location check = source.clone().add(direction.clone().multiply(d));
            if (check.getBlock().getType() == Material.OBSIDIAN) {
                return true;
            }
        }
        return false;
    }
}