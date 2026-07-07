package com.arkonas.ranks.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Small factory for the {@link ItemStack}s shown in menus. Applies the display
 * name and lore and, when requested, a glint override (falling back to a hidden
 * enchant on older APIs).
 */
public final class MenuItems {

  private MenuItems() {
  }

  public static ItemStack build(Material material, Component name, List<Component> lore,
      boolean glow) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      if (name != null) {
        meta.displayName(name);
      }
      if (lore != null && !lore.isEmpty()) {
        meta.lore(lore);
      }
      if (glow) {
        applyGlow(meta);
      }
      item.setItemMeta(meta);
    }
    return item;
  }

  public static void applyGlow(ItemMeta meta) {
    try {
      meta.setEnchantmentGlintOverride(true);
    } catch (Throwable t) {
      try {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      } catch (Throwable ignored) {
        // best effort
      }
    }
  }
}
