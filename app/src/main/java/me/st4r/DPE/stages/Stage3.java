package me.st4r.DPE.stages;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Stage3 {
    private static final Map<Location, BlockData> savedTerrain = new HashMap<>();
    private static BukkitTask cloneTicker = null;
    private static final NamespacedKey CLONE_KEY = new NamespacedKey("dpe", "is_clone");
    private static final NamespacedKey HITS_KEY = new NamespacedKey("dpe", "clone_hits");

    public static void startCloneSpawning(JavaPlugin plugin, EnderDragon trueDragon) {
        if (cloneTicker != null) cloneTicker.cancel();

        cloneTicker = new BukkitRunnable() {
            @Override
            public void run() {
                if (!trueDragon.isValid() || trueDragon.isDead()) {
                    cancel();
                    return;
                }
                
                long cloneCount = trueDragon.getWorld().getEntitiesByClass(EnderDragon.class).stream()
                        .filter(d -> d.getPersistentDataContainer().has(CLONE_KEY))
                        .count();

                if (cloneCount < 1) { 
                    spawnShadowClone(plugin, trueDragon);
                }
            }
        }.runTaskTimer(plugin, 60L, 240L);
    }

    public static void stopCloneSpawning() {
        if (cloneTicker != null) {
            cloneTicker.cancel();
            cloneTicker = null;
        }
        
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) {
                world.getEntitiesByClass(EnderDragon.class).stream()
                        .filter(d -> d.getPersistentDataContainer().has(CLONE_KEY))
                        .forEach(EnderDragon::remove);
            }
        }
    }

    private static void spawnShadowClone(JavaPlugin plugin, EnderDragon parent) {
        Location spawnLoc = parent.getLocation().add(
                ThreadLocalRandom.current().nextDouble(-15, 15),
                4,
                ThreadLocalRandom.current().nextDouble(-15, 15)
        );

        parent.getWorld().spawn(spawnLoc, EnderDragon.class, clone -> {
            var pdc = clone.getPersistentDataContainer();
            pdc.set(CLONE_KEY, PersistentDataType.BOOLEAN, true);
            pdc.set(HITS_KEY, PersistentDataType.INTEGER, 0);
            
            clone.setPhase(EnderDragon.Phase.CHARGE_PLAYER);
            clone.getWorld().playSound(clone.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.4f);
        });
    }

    public static void handleCloneHit(EnderDragon clone) {
        var pdc = clone.getPersistentDataContainer();
        int currentHits = pdc.getOrDefault(HITS_KEY, PersistentDataType.INTEGER, 0) + 1;

        if (currentHits >= 5) {
            clone.getWorld().spawnParticle(Particle.POOF, clone.getLocation(), 80, 2, 2, 2, 0.15);
            clone.getWorld().playSound(clone.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 1.6f);
            clone.remove();
        } else {
            pdc.set(HITS_KEY, PersistentDataType.INTEGER, currentHits);
            clone.getWorld().playSound(clone.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.2f, 0.6f);
            clone.getWorld().spawnParticle(Particle.WITCH, clone.getLocation(), 25, 1, 1, 1, 0.1);
        }
    }

   public static void triggerTerrainShift(Location center) {
        World world = center.getWorld();
        int radius = 15;
        Location base = center.clone();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -10; y <= 10; y++) {
                    if (Math.random() > 0.85) {
                        Location loc = base.clone().add(x, y, z);
                        Block block = loc.getBlock();

                        if (block.getType() == Material.END_STONE || block.getType() == Material.OBSIDIAN) {
                            savedTerrain.putIfAbsent(loc, block.getBlockData());
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    public static void executeEnderPulse(JavaPlugin plugin, Location startLoc, Vector direction, int length) {
        World world = startLoc.getWorld();
        Vector dir = direction.clone().normalize();

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step >= length) {
                    cancel();
                    return;
                }

                Location progressLoc = startLoc.clone().add(dir.clone().multiply(step));
                Block groundBlock = world.getHighestBlockAt(progressLoc).getRelative(0, -1, 0);

                if (groundBlock.getType() != Material.AIR) {
                    BlockData originalData = groundBlock.getBlockData();
                    groundBlock.setType(Material.RED_TERRACOTTA);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        world.spawn(groundBlock.getLocation().add(0, 1, 0), EvokerFangs.class);
                        groundBlock.setBlockData(originalData);

                        world.getNearbyEntities(groundBlock.getLocation(), 2, 2, 2).forEach(entity -> {
                            if (entity instanceof Player p) {
                                p.setVelocity(dir.clone().multiply(2.5).setY(0.5));
                            }
                        });
                    }, 30L);
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public static void restoreTerrain() {
        for (Map.Entry<Location, BlockData> entry : savedTerrain.entrySet()) {
            entry.getKey().getBlock().setBlockData(entry.getValue());
        }
        savedTerrain.clear();
    }
}

//Hello, remember to be kind!

