package com.arkonas.ranks.menu;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Small factory for the {@link ItemStack}s shown in menus. Applies the display
 * name and lore and, when requested, a glint override (falling back to a hidden
 * enchant on older APIs), a custom-model-data value (for resource-pack icons) and
 * a player-head texture (player name or a base64 textures value).
 */
public final class MenuItems {

  private MenuItems() {
  }

  public static ItemStack build(Material material, Component name, List<Component> lore,
      boolean glow) {
    return build(material, name, lore, glow, null, null);
  }

  public static ItemStack build(Material material, Component name, List<Component> lore,
      boolean glow, Integer customModelData, String headTexture) {
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
      if (customModelData != null) {
        try {
          meta.setCustomModelData(customModelData);
        } catch (Throwable ignored) {
          // pre-1.14 server; ignore
        }
      }
      if (headTexture != null && !headTexture.isEmpty() && meta instanceof SkullMeta) {
        applyHeadTexture((SkullMeta) meta, headTexture);
      }
      item.setItemMeta(meta);
    }
    return item;
  }

  /**
   * Applies a head texture to a skull: a short alphanumeric value is treated as a player name, a
   * longer value as a base64 textures property (set reflectively so we do not compile against the
   * server's internal profile classes). Best-effort; failures leave a blank head.
   */
  static void applyHeadTexture(SkullMeta meta, String texture) {
    if (texture.length() <= 16 && texture.matches("[A-Za-z0-9_]+")) {
      try {
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(texture));
        return;
      } catch (Throwable ignored) {
        // fall through to base64 handling
      }
    }
    try {
      Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
      Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
      Object profile = gameProfileClass.getConstructor(UUID.class, String.class)
          .newInstance(UUID.randomUUID(), "arkonasicon");
      Object property = propertyClass.getConstructor(String.class, String.class)
          .newInstance("textures", texture);
      Object properties = gameProfileClass.getMethod("getProperties").invoke(profile);
      properties.getClass().getMethod("put", Object.class, Object.class)
          .invoke(properties, "textures", property);
      java.lang.reflect.Field field = meta.getClass().getDeclaredField("profile");
      field.setAccessible(true);
      field.set(meta, profile);
    } catch (Throwable ignored) {
      // unsupported server or test environment; leave the head blank
    }
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
