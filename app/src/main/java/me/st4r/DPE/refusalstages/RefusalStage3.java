package me.st4r.DPE.refusalstages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;
import java.util.HashSet;


public class RefusalStage3 implements Listener {
    private final JavaPlugin plugin;
    private final EnderDragon dragon;
    private final List<BukkitTask> activeTasks = new ArrayList<>();
    private final Map<Location, BlockData> alteredBlocks = new HashMap<>();
    private final Set<UUID> shadowClones = new HashSet<>();
    
    private final NamespacedKey cloneKey;

    public RefusalStage3(JavaPlugin plugin, EnderDragon dragon) {
        this.plugin = plugin;
        this.dragon = dragon;
        this.cloneKey = new NamespacedKey(plugin, "refusal_shadow_clone");
    }

    public void activate() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startStageLoops();
    }

    public void cleanUp() {
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        activeTasks.forEach(BukkitTask::cancel);
        activeTasks.clear();
        
        shadowClones.forEach(uuid -> {
            var ent = Bukkit.getEntity(uuid);
            if (ent != null) ent.remove();
        });
        shadowClones.clear();
        
        
        alteredBlocks.forEach((loc, data) -> loc.getBlock().setBlockData(data));
        alteredBlocks.clear();
    }

    private void startStageLoops() {
        activeTasks.add(new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!dragon.isValid() || dragon.isDead()) {
                    cancel();
                    return;
                }
                ticks += 20;

                if (ticks % 160 == 0) generateTerrainHoles();
                if (ticks % 300 == 0) manageShadowClones();
                if (ticks % 240 == 0) fireEnderPulse();
                if (ticks % 800 == 0) executeMemoryDrain();
                
                processVoidTendrilsDamage();
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    private void generateTerrainHoles() {
        List<Player> players = dragon.getWorld().getPlayers();
        if (players.isEmpty()) return;
        Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        Location base = target.getLocation().add(ThreadLocalRandom.current().nextDouble(-8, 8), -1, ThreadLocalRandom.current().nextDouble(-8, 8));

        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if ((x*x + z*z) <= radius*radius) {
                    Block block = base.clone().add(x, 0, z).getBlock();
                    if (block.getType() == Material.END_STONE || block.getType() == Material.OBSIDIAN) {
                        alteredBlocks.putIfAbsent(block.getLocation(), block.getBlockData());
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void processVoidTendrilsDamage() {
        alteredBlocks.keySet().forEach(loc -> {
            loc.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(0.5, 0.5, 0.5), 2, 0.2, 0.5, 0.2, 0.01);
            for (Player player : loc.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(loc) < 2.25) { 
                    player.damage(4.0, dragon); 
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 1));
                }
            }
        });
    }

    private void manageShadowClones() {
        shadowClones.removeIf(uuid -> Bukkit.getEntity(uuid) == null || Bukkit.getEntity(uuid).isDead());
        if (shadowClones.size() >= 3) return;

        Location spawnLoc = dragon.getLocation().add(ThreadLocalRandom.current().nextDouble(-15, 15), 5, ThreadLocalRandom.current().nextDouble(-15, 15));
        EnderDragon clone = spawnLoc.getWorld().spawn(spawnLoc, EnderDragon.class, c -> {
            c.getPersistentDataContainer().set(cloneKey, PersistentDataType.BOOLEAN, true);
            c.setPhase(EnderDragon.Phase.CIRCLING);
        });
        shadowClones.add(clone.getUniqueId());


        activeTasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                if (!clone.isValid() || clone.isDead()) { cancel(); return; }
                List<Player> players = clone.getWorld().getPlayers();
                if (players.isEmpty()) return;
                
                Player p = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                Vector direction = p.getLocation().toVector().subtract(clone.getLocation().toVector()).normalize();
                clone.launchProjectile(DragonFireball.class, direction.multiply(1.5));
            }
        }.runTaskTimer(plugin, 40L, 80L));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCloneDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(cloneKey, PersistentDataType.BOOLEAN)) {
            event.setCancelled(true); 
            event.getEntity().getWorld().spawnParticle(Particle.SMOKE, event.getEntity().getLocation(), 20, 1, 1, 1, 0.05);
        }
    }

    private void fireEnderPulse() {
        List<Player> players = dragon.getWorld().getPlayers();
        if (players.isEmpty()) return;
        Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));

        Location start = dragon.getLocation();
        Vector direction = target.getLocation().toVector().subtract(start.toVector()).setY(0).normalize();
        World world = dragon.getWorld();

        activeTasks.add(new BukkitRunnable() {
            int step = 0;
            final int maxSteps = 30;
            @Override
            public void run() {
                if (step >= maxSteps || !dragon.isValid()) { cancel(); return; }

                Location checkLoc = start.clone().add(direction.clone().multiply(step));
                Block block = world.getHighestBlockAt(checkLoc).getRelative(0, -1, 0);

                if (block.getType() != Material.AIR) {
                    BlockData original = block.getBlockData();
                    alteredBlocks.putIfAbsent(block.getLocation(), original);
                    block.setType(Material.RED_TERRACOTTA);

              
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        block.setBlockData(original);
                        Location triggerLoc = block.getLocation().add(0, 1, 0);
                        world.spawn(triggerLoc, EvokerFangs.class);
                        
                        world.getNearbyEntities(triggerLoc, 2, 2, 2).forEach(ent -> {
                            if (ent instanceof Player p) {
                                p.setVelocity(direction.clone().multiply(2.2).setY(0.6));
                            }
                        });
                    }, 36L);
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 2L));
    }

    private void executeMemoryDrain() {
        List<Player> players = dragon.getWorld().getPlayers();
        if (players.isEmpty()) return;
        Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));

        ItemStack[] hotbarBackup = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            hotbarBackup[i] = target.getInventory().getItem(i);
        }

        List<ItemStack> shuffleList = new ArrayList<>();
        for (ItemStack item : hotbarBackup) shuffleList.add(item);
        Collections.shuffle(shuffleList);

        for (int i = 0; i < 9; i++) {
            target.getInventory().setItem(i, shuffleList.get(i));
        }
        target.sendMessage(Component.text("Your muscle memory fails you! Hotbar corrupted!", NamedTextColor.GOLD, TextDecoration.UNDERLINED));
        target.playSound(target.getLocation(), Sound.BLOCK_CHORUS_FLOWER_DEATH, 2.0f, 0.5f);

      
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                for (int i = 0; i < 9; i++) {
                    target.getInventory().setItem(i, hotbarBackup[i]);
                }
                target.sendMessage(Component.text("Your equipment alignment interface stabilizes.", NamedTextColor.GREEN));
            }
        }, 120L);
    }
}