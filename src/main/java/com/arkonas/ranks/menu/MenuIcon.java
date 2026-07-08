package com.arkonas.ranks.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * A resolved icon definition for a menu item: a base material plus optional modern touches — a
 * custom-model-data value (for resource-pack icons from Oraxen / ItemsAdder / Nexo default packs)
 * and a head texture (player name or base64). Parsed from either a plain material string
 * ({@code "DIAMOND"}) or a config section, so existing simple configs keep working.
 */
public final class MenuIcon {

  private final Material material;
  private final Integer customModelData;
  private final String headTexture;
  private final boolean glow;

  public MenuIcon(Material material, Integer customModelData, String headTexture, boolean glow) {
    this.material = material;
    this.customModelData = customModelData;
    this.headTexture = headTexture;
    this.glow = glow;
  }

  public static MenuIcon of(Material material) {
    return new MenuIcon(material, null, null, false);
  }

  /**
   * Parses an icon from a config value that is either a material string or a section with
   * {@code material}, {@code custom-model-data}, {@code head-texture} and {@code glow} keys.
   *
   * @param parent the section holding the value
   * @param key the key to read
   * @param fallback material to use when none is configured or valid
   * @return a resolved icon, never null
   */
  public static MenuIcon parse(ConfigurationSection parent, String key, Material fallback) {
    if (parent == null) {
      return of(fallback);
    }
    if (parent.isConfigurationSection(key)) {
      return fromSection(parent.getConfigurationSection(key), fallback);
    }
    return new MenuIcon(material(parent.getString(key), fallback), null, null, false);
  }

  public static MenuIcon fromSection(ConfigurationSection section, Material fallback) {
    if (section == null) {
      return of(fallback);
    }
    Material material = material(section.getString("material"), fallback);
    Integer cmd = section.contains("custom-model-data")
        ? section.getInt("custom-model-data") : null;
    String head = section.getString("head-texture");
    boolean glow = section.getBoolean("glow", false);
    return new MenuIcon(material, cmd, head, glow);
  }

  public Material material() {
    return material;
  }

  public Integer customModelData() {
    return customModelData;
  }

  public String headTexture() {
    return headTexture;
  }

  public boolean glow() {
    return glow;
  }

  /** Builds the icon item, forcing the glow on if either the config or the caller requests it. */
  public ItemStack build(Component name, List<Component> lore, boolean glowOverride) {
    return MenuItems.build(material, name, lore, glow || glowOverride, customModelData, headTexture);
  }

  private static Material material(String name, Material fallback) {
    if (name == null) {
      return fallback;
    }
    try {
      return Material.valueOf(name.toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return fallback;
    }
  }
}
