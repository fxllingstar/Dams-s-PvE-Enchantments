package me.st4r.DPE;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.chat.hover.content.Item;

import java.util.Arrays;
import java.util.UUID;


public class RefusalRewards {
    
   public static ItemStack createRefusalCleaver() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Sword of the Creator.");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "It did not want to fight.",
            ChatColor.GRAY + "You did not give it a choice.",
            ChatColor.DARK_PURPLE + "Forged from ten deaths."
        ));

        meta.addEnchant(Enchantment.SHARPNESS,        10, true);
        meta.addEnchant(Enchantment.UNBREAKING,        5, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT,       3, true);
        meta.addEnchant(Enchantment.SWEEPING_EDGE,     4, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        
        meta.addAttributeModifier(
            Attribute.ATTACK_SPEED,
            new AttributeModifier(
                UUID.randomUUID(),
                "refusal_speed",
                0.4,
                AttributeModifier.Operation.ADD_NUMBER,
                org.bukkit.inventory.EquipmentSlot.HAND
            )
        );

        sword.setItemMeta(meta);
        return sword;
    }

   public static ItemStack createFracturedWings(){
   
    ItemStack elytra = new ItemStack(Material.ELYTRA);
    ItemMeta meta = elytra.getItemMeta();

    meta.setDisplayName(ChatColor.DARK_PURPLE + "Fractured Wings");
    meta.setLore(Arrays.asList(
        ChatColor.GRAY + "It carries the weight of ten deaths.",
        ChatColor.DARK_PURPLE  + "It will never break."
    ));

    meta.addEnchant(Enchantment.UNBREAKING, 32767, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

     elytra.setItemMeta(meta);
     return elytra;
   }
}
