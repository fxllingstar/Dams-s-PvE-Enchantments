package me.st4r.DPE;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public class KillTracker {
      private static DPE plugin;
      private static NamespacedKey KILL_KEY;
      private static NamespacedKey PEACE_KEY;
      
    
      public static void init(DPE pluginInstance){
        plugin = pluginInstance;
        KILL_KEY = new NamespacedKey(plugin, "dragon_kills");
        PEACE_KEY = new NamespacedKey(plugin, "dragon_peace_chosen");
      }
 
       public static int getKills(Player player){
        return player.getPersistentDataContainer()
        .getOrDefault(KILL_KEY, PersistentDataType.INTEGER, 0);
       }
      public static int incrementAndGet(Player player){
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        int current = pdc.getOrDefault(KILL_KEY, PersistentDataType.INTEGER, 0);
        int next = current + 1;
        pdc.set(KILL_KEY, PersistentDataType.INTEGER, next);
        return next;
      }
     //coop adaptation later
     public static void normalizeToNine(Player player){
        player.getPersistentDataContainer()
        .set(KILL_KEY, PersistentDataType.INTEGER, 9);
     }

     public static boolean hasMadePeace(Player player){
        return player.getPersistentDataContainer()
        .getOrDefault(PEACE_KEY, PersistentDataType.BYTE, (byte) 0) == 1;
     }
    
     public static void setPeaceChosen(Player player) {
        player.getPersistentDataContainer()
        .set(PEACE_KEY, PersistentDataType.BYTE, (byte) 1);
     }
     public static void clearPeace(Player player){
        player.getPersistentDataContainer()
        .remove(PEACE_KEY);
     }
}
