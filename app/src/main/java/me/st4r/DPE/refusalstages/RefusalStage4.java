package me.st4r.DPE.refusalstages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RefusalStage4 implements Listener {
    private final JavaPlugin plugin;
    private final EnderDragon dragon;
    private final List<BukkitTask> activeTasks = new ArrayList<>();
    private final Map<UUID, Long> crystalSpawnTimes = new HashMap<>();
    private final Map<Location, BlockData> rolledArenaBlocks = new HashMap<>();
    
    private UUID currentAggroTarget = null;
    private boolean fractureTriggered = false;

    public RefusalStage4(JavaPlugin plugin, EnderDragon dragon) {
        this.plugin = plugin;
        this.dragon = dragon;
    }

    public void activate() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        applyRageModifiers();
        startStageLoops();
    }

    public void cleanUp() {
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        CreatureSpawnEvent.getHandlerList().unregister(this);
        
        activeTasks.forEach(BukkitTask::cancel);
        activeTasks.clear();
        crystalSpawnTimes.clear();
        
      
        rolledArenaBlocks.forEach((loc, data) -> loc.getBlock().setBlockData(data));
        rolledArenaBlocks.clear();

        
        AttributeInstance moveSpeed = dragon.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveSpeed != null) moveSpeed.setBaseValue(0.7); 
    }

    private void applyRageModifiers() {
        AttributeInstance moveSpeed = dragon.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveSpeed != null) {
            moveSpeed.setBaseValue(moveSpeed.getBaseValue() * 1.8); 
        }
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

                if (ticks % 200 == 0) processPermanentAggroFixation();
                if (ticks % 600 == 0) triggerVoidCollapse();
                
                renderAggroTargetLaser();
                checkFractureCondition();
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    @EventHandler
    public void onCrystalSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof EnderCrystal crystal) {
            crystalSpawnTimes.put(crystal.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShieldedCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
        long time = crystalSpawnTimes.getOrDefault(crystal.getUniqueId(), 0L);
        if (System.currentTimeMillis() - time < 15000) {
            event.setCancelled(true);
            crystal.getWorld().spawnParticle(Particle.CHERRY_LEAVES, crystal.getLocation().add(0,0.5,0), 6);
        }
    }

    private void processPermanentAggroFixation() {
        List<Player> players = dragon.getWorld().getPlayers();
        if (players.isEmpty()) { currentAggroTarget = null; return; }

        Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        currentAggroTarget = target.getUniqueId();
        
        dragon.setTarget(target);
        dragon.setPhase(EnderDragon.Phase.CHARGE_PLAYER);
        target.sendMessage(Component.text("The Dragon locks eyes with you. Malice concentrated!", NamedTextColor.DARK_RED, TextDecoration.BOLD));
    }

    private void renderAggroTargetLaser() {
        if (currentAggroTarget == null) return;
        Player p = Bukkit.getPlayer(currentAggroTarget);
        if (p == null || !p.isOnline() || !p.getWorld().equals(dragon.getWorld())) return;

     
        p.spawnParticle(Particle.CHERRY_LEAVES, p.getLocation().add(0, 2.2, 0), 2, 0.1, 0.1, 0.1, 0);
    }

    private void triggerVoidCollapse() {
        World world = dragon.getWorld();
        Location center = new Location(world, 0, world.getHighestBlockYAt(0, 0), 0); 

        for (Player p : world.getPlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 160, 0));
            p.sendMessage(Component.text("VOID COLLAPSE IMMINENT! CONCEAL BEHIND OBSIDIAN PILLARS!", NamedTextColor.RED, TextDecoration.BOLD));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 0.3f);
        }

       
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : world.getPlayers()) {
                if (isShieldedByPillar(center, p.getLocation())) {
                    p.sendMessage(Component.text("The Obsidian structural framework shielded you from total erasure.", NamedTextColor.GREEN));
                } else {
                    p.damage(16.0, dragon); 
                    p.setVelocity(new Vector(0, 2.6, 0)); 
                    p.spawnParticle(Particle.SONIC_BOOM, p.getLocation().add(0, 1, 0), 1);
                    p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.5f);
                }
            }
            executePillarCrumbleMechanic(center);
        }, 100L);
    }

    private boolean isShieldedByPillar(Location center, Location target) {
        Vector dir = target.toVector().subtract(center.toVector());
        double distance = dir.length();
        dir.normalize();

        for (double d = 2; d < distance; d += 0.5) {
            Location check = center.clone().add(dir.clone().multiply(d));
            if (check.getBlock().getType() == Material.OBSIDIAN) {
                return true;
            }
        }
        return false;
    }

    private void executePillarCrumbleMechanic(Location center) {
        World world = center.getWorld();
        int radius = 60; 
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 40; y < 90; y++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() == Material.OBSIDIAN) {
                        if (ThreadLocalRandom.current().nextDouble() > 0.94) { 
                            rolledArenaBlocks.putIfAbsent(b.getLocation(), b.getBlockData());
                            b.setType(Material.COAL_BLOCK); 
                        }
                    }
                }
            }
        }
    }

    private void checkFractureCondition() {
        if (fractureTriggered) return;
        double hpPercent = dragon.getHealth() / dragon.getAttribute(Attribute.MAX_HEALTH).getValue();
        
        if (hpPercent <= 0.10) { 
            fractureTriggered = true;
            executeFracturePayload();
        }
    }

    private void executeFracturePayload() {
        World world = dragon.getWorld();
        Location center = new Location(world, 0, world.getHighestBlockYAt(0, 0), 0);
        
        for (Player p : world.getPlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 3.0f, 0.2f);
            p.sendMessage(Component.text("THE CORE ARCHITECTURE COLLAPSES. NO ANCHOR POINT REMAINS!", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        }

     
        activeTasks.add(new BukkitRunnable() {
            int currentRingRadius = 0;
            final int maxRadius = 25;
            @Override
            public void run() {
                if (currentRingRadius > maxRadius || !dragon.isValid()) { cancel(); return; }

                for (int x = -currentRingRadius; x <= currentRingRadius; x++) {
                    for (int z = -currentRingRadius; z <= currentRingRadius; z++) {
                        int edgeCheck = x*x + z*z;
                        if (edgeCheck >= (currentRingRadius-1)*(currentRingRadius-1) && edgeCheck <= currentRingRadius*currentRingRadius) {
                            for (int y = -3; y <= 3; y++) {
                                Block b = center.clone().add(x, y, z).getBlock();
                                if (b.getType() == Material.OBSIDIAN || b.getType() == Material.END_STONE || b.getType() == Material.COAL_BLOCK) {
                                    rolledArenaBlocks.putIfAbsent(b.getLocation(), b.getBlockData());
                                    b.setType(Material.AIR);
                                    if (ThreadLocalRandom.current().nextBoolean()) {
                                        b.getWorld().spawnParticle(Particle.FALLING_DUST, b.getLocation(), 2, 0.1, 0.1, 0.1, 0, Material.BLACK_CONCRETE.createBlockData());
                                    }
                                }
                            }
                        }
                    }
                }
                if (currentRingRadius % 3 == 0) {
                    center.getWorld().playSound(center.clone().add(currentRingRadius, 0, 0), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 2.0f, 0.5f);
                }
                currentRingRadius++;
            }
        }.runTaskTimer(plugin, 0L, 4L));
    }
}