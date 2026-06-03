package me.st4r.DPE;
import org.bukkit.plugin.java.JavaPlugin;

public final class DPE extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        DragonManager dragonManager = new DragonManager(this);
        getServer().getPluginManager().registerEvents(dragonManager, this);
         KillTracker.init(this); 

        getLogger().info("Dams's PvE Enchantments (dpe) boss overhaul has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        try {
            Stage3.restoreTerrain();
            Stage4.stopRageMode();
            getLogger().info("Safely restored any shifted End terrain during shutdown.");
        } catch (NoClassDefFoundError | Exception e) { 
            getLogger().warning("Could not verify terrain restoration on shutdown.");
        }

        getLogger().info("Dams's PvE Enchantments (dpe) has been disabled.");
    }
}
