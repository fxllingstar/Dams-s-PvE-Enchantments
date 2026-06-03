package me.st4r.DPE;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class Stage3 {

    private static final Map<Location, BlockData> savedTerrain = new HashMap<>();

    public static void triggerTerrainShift(Location center) {
        World world = center.getWorld();
        int radius = 15;

        Location base = center.clone();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -5; y <= 5; y++) {

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

                    // Telegraph
                    groundBlock.setType(Material.RED_TERRACOTTA);

                    // Delayed strike (only ONE per step)
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
        }.runTaskTimer(plugin, 0L, 2L); // moves forward every 2 ticks
    }

    public static void restoreTerrain() {
        for (Map.Entry<Location, BlockData> entry : savedTerrain.entrySet()) {
            entry.getKey().getBlock().setBlockData(entry.getValue());
        }
        savedTerrain.clear();
    }
}