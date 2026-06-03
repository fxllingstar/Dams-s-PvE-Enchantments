package me.st4r.DPE;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DragonManager implements Listener {
   private final JavaPlugin plugin;
   private final Set<UUID> activeWaveEnderman = new HashSet<>();
   private int currentStage = 1;
   private UUID dragonUUID;
   
    public DragonManager(JavaPlugin plugin){
        this.plugin = plugin;
    }


   @EventHandler
   public void OnDragonSpawn(EntitySpawnEvent event){
    if (!(event.getEntity()instanceof EnderDragon dragon)) return;

    this.dragonUUID = dragon.getUniqueId();
    this.currentStage = 1;

    var maxHealthAttr = dragon.getAttribute(Attribute.MAX_HEALTH);
    if (maxHealthAttr != null){
         maxHealthAttr.setBaseValue(maxHealthAttr.getBaseValue() * 2.0);
         dragon.setHealth(maxHealthAttr.getBaseValue());
        }
        spawnEndermanWave(dragon); 
}

@EventHandler
public void OnDragonDamage(EntityDamageByEntityEvent event){
    if(!(event.getEntity() instanceof EnderDragon dragon)) return;
    if (!dragon.getUniqueId().equals(dragonUUID)) return;

    //Immunity for the dragon
    cleanUpEndermanSet(); 
    if (!activeWaveEnderman.isEmpty()){
        event.setCancelled(true);
        if (event.getDamager() instanceof Player player){
            player.sendMessage("§cThe Ender Dragon is protected by its Endermen thralls!");
        }
    return;
    }
    double maxHealth = dragon.getAttribute(Attribute.MAX_HEALTH).getValue();
    double projectedHealth = dragon.getHealth() - event.getFinalDamage();
    double currentHealthPercent = projectedHealth / maxHealth;
 

    checkPhaseTransition(dragon,currentHealthPercent);
}
private void checkPhaseTransition(EnderDragon dragon, double healthPercent){
if (healthPercent <= 0.75 && currentStage == 1){
    transitionTo(2, dragon);
}else if (healthPercent <= 0.50 && currentStage == 2){
    transitionTo(3, dragon);
}else if (healthPercent <= 0.25 && currentStage == 3){
    transitionTo(4,dragon);
 }
}

private void transitionTo(int newStage, EnderDragon dragon){
    this.currentStage = newStage;
    Bukkit.broadcastMessage("§5§lThe Ender Dragon enters Stage " + newStage + "!");
    spawnEndermanWave(dragon);

        switch (newStage){
        case 2 -> Stage2.triggerVoidCracks(plugin,dragon);
        case 3 -> Stage3.triggerTerrainShift(dragon.getLocation());
        case 4 -> {
            Stage4.startRageMode(plugin, dragon);
            Stage4.runVoidCollapse(plugin, dragon.getLocation());
                }
    }
}   

public void spawnEndermanWave(EnderDragon dragon){
    activeWaveEnderman.clear();
    var world = dragon.getWorld();
    var players = world.getPlayers();
    if (players.isEmpty()) return;

    //Here, replace the code with config file later on.
    //ima make a basic one, just for the test, but CHANGE THIS LATER!!!!!
   for (int i = 0; i < (currentStage * 6); i++){
    var loc = dragon.getLocation().add((Math.random() - 0.5) * 20, -5, (Math.random() - 0.5) * 20);
     var highestBlock = world.getHighestBlockAt(loc);
//highlight enderman in the future
     Enderman enderman = world.spawn(highestBlock.getLocation().add(0,1,0), Enderman.class);
     Player target = players.get((int) (Math.random() * players.size()));
     enderman.setTarget(target);
     activeWaveEnderman.add(enderman.getUniqueId());

    }
  }
   
    private void cleanUpEndermanSet(){
        activeWaveEnderman.removeIf(uuid -> Bukkit.getEntity(uuid) == null|| Bukkit.getEntity(uuid).isDead());
    }
 

    @EventHandler 
    public void OnDragonDeath(EntityDeathEvent event){
        if (event.getEntity() instanceof EnderDragon dragon && event.getEntity().getUniqueId().equals(dragonUUID)){
            Stage3.restoreTerrain();
            Stage4.stopRageMode();
            activeWaveEnderman.clear();
            dragonUUID = null;
            currentStage = 1;
        }
    }
}
