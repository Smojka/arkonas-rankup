package com.arkonas.ranks.menu;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Maps a requirement name to the {@link Material} used for its icon in the menus.
 * Resolution order: exact match on the configured name, then the name with a
 * trailing {@code 'h'} stripped (the "hold" variants such as {@code moneyh} share
 * the {@code money} icon), then the configured default.
 */
public final class RequirementIcons {

  private final Map<String, Material> icons = new HashMap<>();
  private final Material fallback;

  public RequirementIcons(ConfigurationSection section) {
    Material def = Material.PAPER;
    if (section != null) {
      def = material(section.getString("default"), Material.PAPER);
      for (String key : section.getKeys(false)) {
        if (key.equalsIgnoreCase("default")) {
          continue;
        }
        Material material = material(section.getString(key), null);
        if (material != null) {
          icons.put(key.toLowerCase(), material);
        }
      }
    }
    this.fallback = def;
  }

  public Material forRequirement(String name) {
    if (name == null) {
      return fallback;
    }
    String lower = name.toLowerCase();
    Material exact = icons.get(lower);
    if (exact != null) {
      return exact;
    }
    if (lower.endsWith("h") && lower.length() > 1) {
      Material stripped = icons.get(lower.substring(0, lower.length() - 1));
      if (stripped != null) {
        return stripped;
      }
    }
    return fallback;
  }

  private static Material material(String name, Material def) {
    if (name == null) {
      return def;
    }
    try {
      return Material.valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) {
      return def;
    }
  }
}
