package me.st4r.DPE.refusalstages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RefusalStage1 implements Listener {
    private final JavaPlugin plugin;
    private final EnderDragon dragon;
    private final Set<UUID> gateEndermen = new HashSet<>();
    private final List<BukkitTask> activeTasks = new ArrayList<>();
    private final List<Location> phantomHitboxes = new ArrayList<>();
    
    private long lastCrystalBeamTime = 0;

    public RefusalStage1(JavaPlugin plugin, EnderDragon dragon) {
        this.plugin = plugin;
        this.dragon = dragon;
    }

    public void activate() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        spawnGateEndermen();
        startStageLoop();
    }

    public void cleanUp() {
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        activeTasks.forEach(BukkitTask::cancel);
        activeTasks.clear();
        
        gateEndermen.forEach(uuid -> {
            var entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        });
        gateEndermen.clear();
        phantomHitboxes.clear();
    }

    private void spawnGateEndermen() {
        Location loc = dragon.getLocation();
        int count = 4; 
        for (int i = 0; i < count; i++) {
            Location spawnLoc = loc.clone().add(
                ThreadLocalRandom.current().nextDouble(-15, 15),
                -2,
                ThreadLocalRandom.current().nextDouble(-15, 15)
            );
            Location highest = spawnLoc.getWorld().getHighestBlockAt(spawnLoc).getLocation().add(0, 1, 0);
            
            Enderman enderman = highest.getWorld().spawn(highest, Enderman.class, ent -> {
                AttributeInstance hp = ent.getAttribute(Attribute.MAX_HEALTH);
                if (hp != null) {
                    hp.setBaseValue(80.0);
                    ent.setHealth(80.0);
                }
                AttributeInstance kb = ent.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                if (kb != null) {
                    kb.setBaseValue(1.0); 
                }
                
                List<Player> players = dragon.getWorld().getPlayers();
                if (!players.isEmpty()) {
                    ent.setTarget(players.get(ThreadLocalRandom.current().nextInt(players.size())));
                }
            });
            gateEndermen.add(enderman.getUniqueId());
        }
    }

    private void startStageLoop() {
        activeTasks.add(new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!dragon.isValid() || dragon.isDead()) {
                    cancel();
                    return;
                }
                ticks += 20;

                if (ticks % 600 == 0) { 
                    executeShieldBreakYell();
                }
                if (ticks % 200 == 0) {
                    attemptCrystalBeam();
                }
                
                trackPhantomHitboxes();
                checkDragonPerch();
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDragonDamage(EntityDamageByEntityEvent event) {
        if (!event.getEntity().getUniqueId().equals(dragon.getUniqueId())) return;
        

        gateEndermen.removeIf(uuid -> Bukkit.getEntity(uuid) == null || Bukkit.getEntity(uuid).isDead());
        
        if (!gateEndermen.isEmpty()) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
                player.sendMessage(Component.text("The Dragon's protection gate is active! Eliminate the resilient Endermen first.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            }
        }
    }

    private void executeShieldBreakYell() {
        for (Player player : dragon.getWorld().getPlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 4.0f, 0.4f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1)); 
            
            player.sendMessage(Component.text("The Dragon's piercing yell shatters your guard!", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        }
    }

    private void attemptCrystalBeam() {
        long now = System.currentTimeMillis();
        if (now - lastCrystalBeamTime < 25000) return; 

        List<EnderCrystal> crystals = new ArrayList<>(dragon.getWorld().getEntitiesByClass(EnderCrystal.class));
        List<Player> players = dragon.getWorld().getPlayers();
        if (crystals.isEmpty() || players.isEmpty()) return;

        EnderCrystal crystal = crystals.get(ThreadLocalRandom.current().nextInt(crystals.size()));
        Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        
        lastCrystalBeamTime = now;
        executeCrystalBeam(crystal, target);
    }

    private void executeCrystalBeam(EnderCrystal crystal, Player target) {
        Location origin = crystal.getLocation().add(0, 0.5, 0);
        target.playSound(target.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);

       
        BukkitTask telegraph = new BukkitRunnable() {
            int runs = 0;
            @Override
            public void run() {
                if (!target.isOnline() || !target.getWorld().equals(origin.getWorld()) || runs++ > 15) {
                    cancel();
                    return;
                }
                target.spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 8, 0.2, 0.2, 0.2, 0.1);
                target.spawnParticle(Particle.DRAGON_BREATH, origin, 5, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 2L);
        activeTasks.add(telegraph);


        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            telegraph.cancel();
            if (!target.isOnline() || !target.getWorld().equals(origin.getWorld())) return;

            Location targetLoc = target.getLocation().add(0, 1.0, 0);
            Vector direction = targetLoc.toVector().subtract(origin.toVector());
            double dist = direction.length();
            direction.normalize();


            for (double d = 0; d < dist; d += 0.5) {    
                Location pPoint = origin.clone().add(direction.clone().multiply(d));
                target.spawnParticle(Particle.ENCHANTED_HIT, pPoint, 1, 0, 0, 0, 0);
            }
            if (target.getLocation().distanceSquared(targetLoc) < 16.0) {
                target.damage(12.0, dragon); 
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                target.playSound(target.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.5f);
            }
        }, 30L);
    }

    private void checkDragonPerch() {
        if (dragon.getPhase() == EnderDragon.Phase.LAND_ON_PORTAL) {
            Location perchLoc = dragon.getLocation();
            if (phantomHitboxes.stream().noneMatch(loc -> loc.distanceSquared(perchLoc) < 4.0)) {
                phantomHitboxes.add(perchLoc.clone());
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> phantomHitboxes.remove(perchLoc), 60L);
            }
        }
    }

    private void trackPhantomHitboxes() {
        if (phantomHitboxes.isEmpty()) return;
        for (Player player : dragon.getWorld().getPlayers()) {
            Location pLoc = player.getLocation();
            for (Location hitbox : phantomHitboxes) {
                if (pLoc.distanceSquared(hitbox) < 5.0) {
                    player.damage(6.0, dragon); 
                    player.spawnParticle(Particle.WITCH, pLoc.add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.5f, 0.2f);
                }
            }
        }
    }
}