package com.arkonas.ranks.menu;

import java.io.File;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import com.arkonas.ranks.ArkonasRanksPlugin;

/**
 * Loads {@code menus.yml} and exposes typed accessors for the theme, animation
 * and per-menu layout blocks. Reloaded whenever the plugin reloads.
 */
public class MenuConfig {

  private final YamlConfiguration config;

  public MenuConfig(ArkonasRanksPlugin plugin) {
    File file = new File(plugin.getDataFolder(), "menus.yml");
    if (!file.exists()) {
      plugin.saveResource("menus.yml", false);
    }
    this.config = YamlConfiguration.loadConfiguration(file);
  }

  public ConfigurationSection theme() {
    return section("theme");
  }

  public ConfigurationSection animation() {
    return section("animation");
  }

  public ConfigurationSection requirementIcons() {
    return section("requirement-icons");
  }

  /**
   * @param name one of hub/path/rankup/prestige/prestige-list/leaderboard
   */
  public ConfigurationSection menu(String name) {
    return section("menus." + name);
  }

  public boolean animationEnabled() {
    ConfigurationSection animation = animation();
    return animation == null || animation.getBoolean("enabled", true);
  }

  public long animationPeriod() {
    ConfigurationSection animation = animation();
    long period = animation == null ? 2 : animation.getLong("period", 2);
    return Math.max(1, period);
  }

  public boolean animationFlag(String key) {
    ConfigurationSection animation = animation();
    return animation != null && animation.getBoolean(key, true);
  }

  public int rows(String menu, int def) {
    ConfigurationSection section = menu(menu);
    return section == null ? def : section.getInt("rows", def);
  }

  public int slot(String menu, String key, int def) {
    ConfigurationSection section = menu(menu);
    return section == null ? def : section.getInt(key, def);
  }

  public List<Integer> slots(String menu, String key) {
    ConfigurationSection section = menu(menu);
    return section == null ? List.of() : section.getIntegerList(key);
  }

  private ConfigurationSection section(String path) {
    return config.getConfigurationSection(path);
  }
}
