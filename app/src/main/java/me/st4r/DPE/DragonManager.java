package me.st4r.DPE;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.st4r.DPE.stages.Stage1;
import me.st4r.DPE.stages.Stage2;
import me.st4r.DPE.stages.Stage3;
import me.st4r.DPE.stages.Stage4;

import org.bukkit.persistence.PersistentDataType;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DragonManager implements Listener {
    private final JavaPlugin plugin;
    private final Set<UUID> activeWaveEnderman = new HashSet<>();
    private int currentStage = 1;
    private UUID dragonUUID;
    
    private NamespacedKey stageKey;
   
    public DragonManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.stageKey = new NamespacedKey(plugin, "dragon_stage");
    }

    @EventHandler
    public void onDragonSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (isCloneDragon(dragon)) return;

        initializeDragon(dragon, dragon.getPersistentDataContainer().has(stageKey, PersistentDataType.INTEGER));
    }

    public void restoreExistingDragon(EnderDragon dragon) {
        if (isCloneDragon(dragon)) {
            return;
        }
        initializeDragon(dragon, dragon.getPersistentDataContainer().has(stageKey, PersistentDataType.INTEGER));
    }

    private boolean isCloneDragon(EnderDragon dragon) {
        return dragon.getPersistentDataContainer().has(new NamespacedKey(plugin, "is_clone"));
    }

    public void ensureTrackedDragon() {
        if (dragonUUID != null) {
            var tracked = Bukkit.getEntity(dragonUUID);
            if (tracked instanceof EnderDragon dragon && dragon.isValid() && !isCloneDragon(dragon)) {
                return;
            }
        }

        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.THE_END) continue;
            world.getEntitiesByClass(EnderDragon.class).stream()
                    .filter(dragon -> !isCloneDragon(dragon))
                    .findFirst()
                    .ifPresent(this::restoreExistingDragon);
        }
    }

    private void initializeDragon(EnderDragon dragon, boolean resumeFromPdc) {
        this.dragonUUID = dragon.getUniqueId();
        var pdc = dragon.getPersistentDataContainer();

        if (resumeFromPdc) {
            this.currentStage = pdc.getOrDefault(stageKey, PersistentDataType.INTEGER, 1);
            resumeStageTasks(dragon);
            return;
        }

        this.currentStage = 1;
        pdc.set(stageKey, PersistentDataType.INTEGER, 1);
        stopAllStageTasks();

        var maxHealthAttr = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(maxHealthAttr.getBaseValue() * 2.0);
            dragon.setHealth(maxHealthAttr.getBaseValue());
        }
        
        spawnEndermanWave(dragon);
        Stage1.startStageTasks(plugin, dragon);
    }

    private void stopAllStageTasks() {
        Stage1.stopStageTasks();
        Stage2.stopStage2Mechanics();
        Stage3.stopCloneSpawning();
        Stage4.stopRageMode();
    }

    private void resumeStageTasks(EnderDragon dragon) {
        switch (this.currentStage) {
            case 1 -> Stage1.startStageTasks(plugin, dragon);
            case 2 -> Stage2.startStage2Mechanics(plugin, dragon);
            case 3 -> Stage3.startCloneSpawning(plugin, dragon);
            case 4 -> Stage4.startRageMode(plugin, dragon);
        }
    }

    @EventHandler
    public void onDragonDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        
        if (dragon.getPersistentDataContainer().has(new NamespacedKey(plugin, "is_clone"))) {
            event.setCancelled(true);
            Stage3.handleCloneHit(dragon);
            return;
        }

        if (!dragon.getUniqueId().equals(dragonUUID)) return;

        if (!activeWaveEnderman.isEmpty()) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
                player.sendMessage(Component.text("The Ender Dragon is protected by its Endermen!", NamedTextColor.RED));
            }
            return;
        }

        double maxHealth = dragon.getAttribute(Attribute.MAX_HEALTH).getValue();
        double projectedHealth = dragon.getHealth() - event.getFinalDamage();
        double currentHealthPercent = projectedHealth / maxHealth;
 
        checkPhaseTransition(dragon, currentHealthPercent);
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event) {
        activeWaveEnderman.remove(event.getEntity().getUniqueId());
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) return;

        long alivePlayersLeft = player.getWorld().getPlayers().stream()
                .filter(p -> !p.isDead() && !p.getUniqueId().equals(player.getUniqueId()))
                .count();

        if (alivePlayersLeft == 0) {
            for (UUID uuid : activeWaveEnderman) {
                var entity = Bukkit.getEntity(uuid);
                if (entity != null) entity.remove();
            }
            activeWaveEnderman.clear();
        }
    }

    @EventHandler
    public void onDragonHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !dragon.getUniqueId().equals(dragonUUID)) return;

        if (currentStage == 1 && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.ENDER_CRYSTAL) {
            event.setAmount(event.getAmount() * 2.5);
        }
    }

    private void checkPhaseTransition(EnderDragon dragon, double healthPercent) {
        if (healthPercent <= 0.75 && currentStage == 1) {
            transitionTo(2, dragon);
        } else if (healthPercent <= 0.50 && currentStage == 2) {
            transitionTo(3, dragon);
        } else if (healthPercent <= 0.25 && currentStage == 3) {
            transitionTo(4, dragon);
        }
    }

    private void transitionTo(int newStage, EnderDragon dragon) {
        this.currentStage = newStage;
        dragon.getPersistentDataContainer().set(stageKey, PersistentDataType.INTEGER, newStage);
        
        Bukkit.broadcast(Component.text("The Ender Dragon enters Stage " + newStage + "!", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        spawnEndermanWave(dragon);

        switch (newStage) {
            case 2 -> {
                Stage1.stopStageTasks();
                Stage2.startStage2Mechanics(plugin, dragon);
            }
            case 3 -> {
                Stage2.stopStage2Mechanics();
                Stage3.triggerTerrainShift(dragon.getLocation());
                Stage3.startCloneSpawning(plugin, dragon);
            }
            case 4 -> {
                Stage3.stopCloneSpawning();
                Stage4.startRageMode(plugin, dragon);
                Stage4.runVoidCollapse(plugin, dragon.getLocation());
            }
        }
    }   

    public void spawnEndermanWave(EnderDragon dragon) {
        activeWaveEnderman.clear();
        var world = dragon.getWorld();
        var players = world.getPlayers();
        if (players.isEmpty()) return;

        var config = plugin.getConfig();
        boolean useFixed = config.getBoolean("endermen-settings.use-fixed-amount", false);
        int totalToSpawn;

        if (useFixed) {
            int fixedMultiplier = config.getInt("endermen-settings.fixed-multiplier", 6);
            totalToSpawn = currentStage * fixedMultiplier;
        } else {
            int perPlayerMultiplier = config.getInt("endermen-settings.multiplier-per-player", 1);
            totalToSpawn = currentStage * players.size() * perPlayerMultiplier;
        }

        for (int i = 0; i < totalToSpawn; i++) {
            var loc = dragon.getLocation().add((Math.random() - 0.5) * 20, -5, (Math.random() - 0.5) * 20);
            var highestBlock = world.getHighestBlockAt(loc);

            Enderman enderman = world.spawn(highestBlock.getLocation().add(0, 1, 0), Enderman.class, ent -> {
                Player target = players.get((int) (Math.random() * players.size()));
                ent.setTarget(target);

                if (currentStage >= 2) {
                    ent.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1));
                    ent.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1));
                }
            });
            activeWaveEnderman.add(enderman.getUniqueId());
        }
    }
 
    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon && event.getEntity().getUniqueId().equals(dragonUUID)) {

            Stage3.restoreTerrain();
            Stage4.stopRageMode();
            activeWaveEnderman.clear();
            dragonUUID = null;
            currentStage = 1;

            Player killer = dragon.getKiller();
            if (killer != null) {
                if (KillTracker.hasMadePeace(killer)) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        killer.sendMessage(ChatColor.DARK_PURPLE + "[Dragon] " + ChatColor.LIGHT_PURPLE + "...");
                        killer.sendMessage(ChatColor.DARK_PURPLE + "[Dragon] " + ChatColor.LIGHT_PURPLE + "Good fight, Traveler.");
                        killer.sendMessage(ChatColor.DARK_PURPLE + "[Dragon] " + ChatColor.GRAY + "Until the next cycle.");
                    }, 40L);
                    return;
                }

                int kills = KillTracker.incrementAndGet(killer);
                if (kills >= 10) {
                    event.setCancelled(true);
                    triggerRefusal(killer, dragon);
                }
            }
        }
    }

    private void triggerRefusal(Player killer, EnderDragon dragon) {
        RefusalAdvancement.grant(killer);
        dragon.setHealth(dragon.getMaxHealth());

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            new RefusalDialogue((DPE) plugin, killer, dragon).start();
        }, 40L);
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        World newWorld = player.getWorld();

        if (newWorld.getEnvironment() != World.Environment.THE_END) return;
        if (!KillTracker.hasMadePeace(player)) return;

        EnderDragon dragon = newWorld.getEntitiesByClass(EnderDragon.class)
            .stream()
            .findFirst()
            .orElse(null);

        if (dragon == null) return;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            new ReturnDialogue((DPE) plugin, player, dragon).start();
        }, 60L);
    }
}
