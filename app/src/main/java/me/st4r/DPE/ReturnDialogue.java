package me.st4r.DPE;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ReturnDialogue  implements Listener{

    private final DPE plugin;
    private final Player player;
    private final EnderDragon dragon;
    
    private static UUID activePlayer = null;

    public ReturnDialogue(DPE plugin, Player player, EnderDragon dragon){
        this.plugin = plugin;
        this.player = player;
        this.dragon = dragon;
    }

    public void start(){
        activePlayer = player.getUniqueId();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        dragon.setAI(false);
        dragon.setInvulnerable(true);

        String prefix = ChatColor.DARK_PURPLE + "[Dragon] " + ChatColor.LIGHT_PURPLE;

        String[] lines = {
        prefix + "You again...",
        prefix + "I remember you.",
        prefix + "Why have you returned?"
        };

        for (int i = 0; i < lines.length; i++){
            final String line = lines[i];
            final boolean isLast = (i == lines.length - 1);
            
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.sendMessage(line);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 0.7f);

                if (isLast) promptChoice();
            },i * 40L);
        }
    }

    private void promptChoice(){
        player.sendMessage("");
        player.sendMessage("[FIGHT] " + ChatColor.GRAY + "Type 'fight' for a battle, just like the old times..");
        player.sendMessage(ChatColor.AQUA + "[LEAVE] " + ChatColor.GRAY + "Type 'leave' if you do not want to fight.");
        player.sendMessage("");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void  onChat(AsyncPlayerChatEvent e){
        if (!e.getPlayer().getUniqueId().equals(activePlayer)) return;
         e.setCancelled(true);
         String msg = e.getMessage().toLowerCase().trim();

         if(msg.equals("fight")){
            Bukkit.getScheduler().runTask(plugin, this::resolveFight);
        }else if (msg.equals("leave")){
            Bukkit.getScheduler().runTask(plugin, this::resolveLeave);
        }else {
            Bukkit.getScheduler().runTask(plugin, () -> 
            player.sendMessage(ChatColor.DARK_GRAY + ChatColor.ITALIC.toString() + "The dragon awaits."));
        }
    }
    private void resolveFight(){
        cleanup();

        player.sendMessage(ChatColor.DARK_RED + "[Dragon] " + ChatColor.GRAY + "Very well.");
         player.sendMessage(ChatColor.DARK_RED + "[Dragon] " + ChatColor.GRAY + "Good luck.");

         Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
            dragon.setAI(true);
            dragon.setInvulnerable(false);
         }, 60L);
    }

    private void resolveLeave(){
        cleanup();

        player.sendMessage(ChatColor.LIGHT_PURPLE + "[Dragon] " + ChatColor.GRAY + "Stay safe.");
         Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            dragon.setAI(false);
          
        }, 40L);    
    }

    private void cleanup(){
        AsyncPlayerChatEvent.getHandlerList().unregister(this);
        activePlayer = null;
        dragon.setInvulnerable(false);
    }
}