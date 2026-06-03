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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;



public class RefusalDialogue implements Listener {

    private final DPE plugin;
    private final Player player;
    private final EnderDragon dragon;

    private static UUID activePlayer = null;
    public RefusalDialogue(DPE plugin, Player player, EnderDragon dragon){
        this.plugin = plugin;
        this.player = player;
        this.dragon = dragon;
    }
    public void start(){
        activePlayer = player.getUniqueId();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        //Lock the dragon in place during yapping session
        dragon.setAI(false);
        dragon.setInvulnerable(true);

        String prefix = ChatColor.DARK_PURPLE + "[???]" + ChatColor.LIGHT_PURPLE;

        String[] lines = {
        prefix + "So. It ends here. Again...",
        prefix + "Do you ever wonder why I keep returning?",
        prefix + "I wonder the same about you...",
        prefix + "We are not so different, traveler.",
        prefix + "Perhaps this cycle does not need to continue.",
        ChatColor.DARK_BLUE + "What shall it be?"
        };

        for (int i = 0; i < lines.length; i++){
            final String line = lines[i];
            final boolean isLast = (i == lines.length - 1);
            

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.sendMessage(line);
                player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 0.6f
                );
              if (isLast){
                promptChoice();
              }
            }, i * 40L);
        }
    }

private void promptChoice(){
    player.sendMessage("");
    player.sendMessage(
        ChatColor.GREEN + "[PEACE]" + ChatColor.GRAY + "Type 'peace' in chat."
    );
    player.sendMessage(
        ChatColor.RED + "[FIGHT]" + ChatColor.GRAY + "Type 'fight' in chat."
    );
     player.sendMessage("");
}

 @EventHandler(priority = EventPriority.HIGHEST)
 public void onChat (AsyncPlayerChatEvent e){

    if(!e.getPlayer().getUniqueId().equals(activePlayer)) return;

    e.setCancelled(true);
    String msg = e.getMessage().toLowerCase().trim();

    if(msg.equals("peace")){
        Bukkit.getScheduler().runTask(plugin, () -> resolvePeace());
    }else if (msg.equals("fight")){
        Bukkit.getScheduler().runTask(plugin, () -> resolveFight());
    }else{
        Bukkit.getScheduler().runTask(plugin, () -> 
     player.sendMessage(
        ChatColor.DARK_GRAY + ChatColor.ITALIC.toString() + "The dragon waits for an answer..."
      ));
     
    }
 }

 private void resolvePeace(){
    cleanup();

    player.sendMessage(ChatColor.LIGHT_PURPLE + "[Dragon]" + ChatColor.GRAY + "A wise choice.");
     player.sendMessage(ChatColor.LIGHT_PURPLE + "[Dragon]" + ChatColor.GRAY + "The end will know peace.");


         Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            player.getInventory().addItem(RefusalRewards.createFracturedWings());
            player.sendMessage(ChatColor.DARK_PURPLE +
                "✦ You received: " + ChatColor.LIGHT_PURPLE + "Fractured Wings");

            // Grant achievement toast
            // TODO: Add the Achivement with Achivement API in a seperate file

            dragon.remove();
            KillTracker.normalizeToNine(player);

        }, 60L);
    }

        private void resolveFight() {
        cleanup();

        player.sendMessage(ChatColor.DARK_RED +
            "[Dragon] " + ChatColor.GRAY + "Farewell, traveler.");
        player.sendMessage(ChatColor.DARK_RED +
            "[Dragon] " + ChatColor.GRAY + "Then we end this. Properly.");

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            dragon.setAI(true);
            dragon.setInvulnerable(false);

            // TODO: hook into refusal fight stages here
            // RefusalBattle.start(plugin, player, dragon);

        }, 60L);
    }

    private void cleanup() {
        AsyncPlayerChatEvent.getHandlerList().unregister(this);
        activePlayer = null;

        dragon.setInvulnerable(false);
    }
}


