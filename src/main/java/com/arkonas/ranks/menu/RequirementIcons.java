package com.arkonas.ranks.menu;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Maps a requirement name to the {@link MenuIcon} used for its icon in the menus. Each entry may be
 * a plain material string (e.g. {@code money: GOLD_INGOT}) or a section with
 * {@code material}, {@code custom-model-data}, {@code head-texture} and {@code glow} for modern
 * resource-pack icons. Resolution order: exact match on the configured name, then the name with a
 * trailing {@code 'h'} stripped (the "hold" variants such as {@code moneyh} share the {@code money}
 * icon), then the configured default.
 */
public final class RequirementIcons {

  private final Map<String, MenuIcon> icons = new HashMap<>();
  private final MenuIcon fallback;

  public RequirementIcons(ConfigurationSection section) {
    MenuIcon def = MenuIcon.of(Material.PAPER);
    if (section != null) {
      def = MenuIcon.parse(section, "default", Material.PAPER);
      for (String key : section.getKeys(false)) {
        if (key.equalsIgnoreCase("default")) {
          continue;
        }
        MenuIcon icon = MenuIcon.parse(section, key, null);
        if (icon.material() != null) {
          icons.put(key.toLowerCase(), icon);
        }
      }
    }
    this.fallback = def;
  }

  public MenuIcon iconFor(String name) {
    if (name == null) {
      return fallback;
    }
    String lower = name.toLowerCase();
    MenuIcon exact = icons.get(lower);
    if (exact != null) {
      return exact;
    }
    if (lower.endsWith("h") && lower.length() > 1) {
      MenuIcon stripped = icons.get(lower.substring(0, lower.length() - 1));
      if (stripped != null) {
        return stripped;
      }
    }
    return fallback;
  }

  /** The icon material for a requirement (back-compat accessor). */
  public Material forRequirement(String name) {
    return iconFor(name).material();
  }
}
