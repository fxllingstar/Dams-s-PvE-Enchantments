package me.st4r.DPE;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.plugin.java.JavaPlugin;

import me.st4r.DPE.stages.Stage1;
import me.st4r.DPE.stages.Stage2;
import me.st4r.DPE.stages.Stage3;
import me.st4r.DPE.stages.Stage4;

public final class DPE extends JavaPlugin {
    private DragonManager dragonManager;

    @Override
    public void onEnable() {
        saveDefaultConfig(); 
        dragonManager = new DragonManager(this); 
        getServer().getPluginManager().registerEvents(dragonManager, this); 

        Bukkit.getScheduler().runTask(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() != World.Environment.THE_END) continue;
                world.getEntitiesByClass(EnderDragon.class).stream()
                        .filter(dragon -> !dragon.getPersistentDataContainer().has(new NamespacedKey(this, "is_clone")))
                        .findFirst()
                        .ifPresent(dragonManager::restoreExistingDragon);
            }
        });
        
        
        KillTracker.init(this); 
        RefusalAdvancement.register(this); 
        
        getLogger().info("Dams's PvE Enchantments (dpe) boss overhaul has been successfully enabled!"); 
    }

    @Override
    public void onDisable() {
        try {
      
            Stage3.restoreTerrain(); 
            
 
            Stage1.stopStageTasks();
            Stage2.stopStage2Mechanics();
            Stage3.stopCloneSpawning();
            Stage4.stopRageMode(); 
            
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.THE_END) {
                    world.getEntitiesByClass(EnderDragon.class).stream()
                        .filter(dragon -> dragon.getPersistentDataContainer().has(new NamespacedKey(this, "is_clone")))
                        .forEach(EnderDragon::remove);
                }
            }
            getLogger().info("Safely restored any shifted End terrain and purged clone entities during shutdown."); 
        } catch (NoClassDefFoundError | Exception e) { 
            getLogger().warning("Could not verify landscape state updates cleanly on shutdown."); 
        }
        
        RefusalAdvancement.unregister(); 
        getLogger().info("Dams's PvE Enchantments (dpe) has been disabled.");
    }
}
