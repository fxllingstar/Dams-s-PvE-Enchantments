package me.st4r.DPE;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

public class RefusalAdvancement {
     private static final String KEY = "but_it_refused";
    private static NamespacedKey advancementKey;

    @SuppressWarnings("deprecation")
    public static void register (DPE plugin){
        advancementKey = new NamespacedKey(plugin, KEY);

        if (Bukkit.getAdvancement(advancementKey) != null)return;
         String json = """
            {
              "display": {
                "icon": {
                  "id": "minecraft:dragon_egg"
                },
                "title": {
                  "text": "But... It refused.",
                  "color": "dark_purple"
                },
                "description": {
                  "text": "The dragon remembered you.",
                  "color": "gray"
                },
                "frame": "challenge",
                "show_toast": true,
                "announce_to_chat": true,
                "hidden": true
              },
              "criteria": {
                "trigger_manually": {
                  "trigger": "minecraft:impossible"
                }
              }
            }
            """;
            try{
                Bukkit.getUnsafe().loadAdvancement(advancementKey, json);
                plugin.getLogger().info("DPE Successfully registered advancements.");
            } catch (IllegalArgumentException e){
                plugin.getLogger().severe("DPE failed to register advancecments: " + e.getMessage());

            }   
    }
    public static void grant(Player player){
        if (advancementKey == null) return;
        Advancement advancement = Bukkit.getAdvancement(advancementKey);
        if (advancement == null){
            player.sendMessage("Advancement failed to register/failed.");
            return;
        }
     AdvancementProgress progress = player.getAdvancementProgress(advancement);

     if(progress.isDone()) return;
     progress.awardCriteria("trigger_manually");



    }
     @SuppressWarnings("deprecation")

     public static void unregister(){
        if (advancementKey == null) return;
        try{
            Bukkit.getUnsafe().removeAdvancement(advancementKey);
        }catch (Exception ignored){}
     }

}
