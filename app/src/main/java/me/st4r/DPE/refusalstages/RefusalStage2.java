package me.st4r.DPE.refusalstages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
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

public class RefusalStage2 implements Listener {
    private final JavaPlugin plugin;
    private final EnderDragon dragon;
    private final List<BukkitTask> activeTasks = new ArrayList<>();
    private final Map<UUID, Long> crystalSpawnTimes = new HashMap<>();
    private final Map<UUID, Long> endermanSpawnTimes = new HashMap<>();
    private final List<Location> gravityWells = new ArrayList<>();

    public RefusalStage2(JavaPlugin plugin, EnderDragon dragon) {
        this.plugin = plugin;
        this.dragon = dragon;
    }

    public void activate() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startStageLoops();
        spawnReinforcements();
    }

    public void cleanUp() {
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        EntityDeathEvent.getHandlerList().unregister(this);
        CreatureSpawnEvent.getHandlerList().unregister(this);
        
        activeTasks.forEach(BukkitTask::cancel);
        activeTasks.clear();
        crystalSpawnTimes.clear();
        endermanSpawnTimes.clear();
        gravityWells.clear();
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
                ticks += 2;

                if (ticks % 300 == 0) {
                    generateGravityWell();
                }
                if (ticks % 500 == 0) {
                    executeInvisibleDive();
                }
                
                processGravityWellForces();
            }
        }.runTaskTimer(plugin, 0L, 2L));
    }

    private void spawnReinforcements() {
        int currentEndermen = 0;
        for (Enderman ent : dragon.getWorld().getEntitiesByClass(Enderman.class)) {
            if (ent.isValid()) currentEndermen++;
        }
        
        int toSpawn = Math.min(6 - currentEndermen, 4); 
        for (int i = 0; i < toSpawn; i++) {
            Location loc = dragon.getLocation().add(ThreadLocalRandom.current().nextDouble(-20, 20), -3, ThreadLocalRandom.current().nextDouble(-20, 20));
            Location high = loc.getWorld().getHighestBlockAt(loc).getLocation().add(0, 1, 0);
            
            high.getWorld().spawn(high, Enderman.class, enderman -> {
                enderman.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1));
                endermanSpawnTimes.put(enderman.getUniqueId(), System.currentTimeMillis());
                
                List<Player> targets = dragon.getWorld().getPlayers();
                if (!targets.isEmpty()) enderman.setTarget(targets.get(ThreadLocalRandom.current().nextInt(targets.size())));
            });
        }
    }

    @EventHandler
    public void onCrystalSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof EnderCrystal crystal) {
            crystalSpawnTimes.put(crystal.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
        
        long spawnTime = crystalSpawnTimes.getOrDefault(crystal.getUniqueId(), 0L);
        if (System.currentTimeMillis() - spawnTime < 15000) { 
            event.setCancelled(true);
            crystal.getWorld().spawnParticle(Particle.CHERRY_LEAVES, crystal.getLocation().add(0, 0.5, 0), 10, 0.4, 0.4, 0.4, 0.05);
            if (event.getDamager() instanceof Player player) {
                player.sendMessage(Component.text("This Crystal is shielded by a temporal barrier!", NamedTextColor.LIGHT_PURPLE));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.5f);
            }
        }
    }

    @EventHandler
    public void onEndermanDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Enderman enderman)) return;
        UUID uuid = enderman.getUniqueId();
        
        if (endermanSpawnTimes.containsKey(uuid)) {
            long lifetime = System.currentTimeMillis() - endermanSpawnTimes.remove(uuid);
            if (lifetime <= 5000) { 
                Location loc = enderman.getLocation();
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
                
                for (Player p : loc.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(loc) < 36.0) {
                        p.damage(10.0, dragon); 
                        p.setVelocity(p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.2).setY(0.4));
                    }
                }
            }
        }
    }

    private void generateGravityWell() {
        List<Player> players = dragon.getWorld().getPlayers();
        if (players.isEmpty()) return;
        
        Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        Location wellLoc = target.getLocation().add(ThreadLocalRandom.current().nextDouble(-10, 10), 0, ThreadLocalRandom.current().nextDouble(-10, 10));
        
        gravityWells.add(wellLoc);
        Bukkit.getScheduler().runTaskLater(plugin, () -> gravityWells.remove(wellLoc), 240L);
    }

    private void processGravityWellForces() {
        for (Location well : gravityWells) {
            boolean isPull = (well.getBlockX() % 2 == 0); 
            well.getWorld().spawnParticle(isPull ? Particle.PORTAL : Particle.REVERSE_PORTAL, well, 6, 0.5, 0.1, 0.5, 0.05);
            
            for (Player player : well.getWorld().getPlayers()) {
                double distSq = player.getLocation().distanceSquared(well);
                if (distSq < 144.0 && distSq > 1.0) {
                    double distance = Math.sqrt(distSq);
                    double intensity = (12.0 - distance) / 12.0;
                    
                    Vector vector = player.getLocation().toVector().subtract(well.toVector());
                    Vector adjustment = isPull ? vector.clone().normalize().multiply(-0.18 * intensity) : vector.clone().normalize().multiply(0.25 * intensity);
                    
                    player.setVelocity(player.getVelocity().add(adjustment.setY(isPull ? -0.02 : 0.08)));
                }
            }
        }
    }

    private void executeInvisibleDive() {
        List<Player> players = dragon.getWorld().getPlayers();
        if (players.isEmpty()) return;
        Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));

        dragon.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false));
        Location targetTrack = target.getLocation();

        BukkitTask trackingShadow = new BukkitRunnable() {
            int runs = 0;
            @Override
            public void run() {
                if (runs++ > 15) { cancel(); return; }
                targetTrack.getWorld().spawnParticle(Particle.SQUID_INK, targetTrack, 25, 1.5, 0, 1.5, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 2L);
        activeTasks.add(trackingShadow);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            trackingShadow.cancel();
            if (!dragon.isValid() || dragon.isDead()) return;

            dragon.teleport(targetTrack.clone().add(0, 1, 0));
            targetTrack.getWorld().playSound(targetTrack, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
            targetTrack.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetTrack, 3);

            for (Player p : targetTrack.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(targetTrack) < 49.0) { 
                    p.damage(16.0, dragon);
                    p.setVelocity(new Vector(0, 1.6, 0)); 
                }
            }
            dragon.removePotionEffect(PotionEffectType.INVISIBILITY);
        }, 30L);
    }
}