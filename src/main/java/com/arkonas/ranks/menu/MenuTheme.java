package com.arkonas.ranks.menu;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.arkonas.ranks.ArkonasRanksPlugin;

/**
 * The resolved visual theme: gradient endpoints, state colours, the
 * unicode {@link ProgressBar} and the pre-built border {@link ItemStack}s
 * (one opaque base plus a light variant per configured colour). Rebuilding the
 * border items happens once per reload so the ticker allocates zero item stacks
 * per frame.
 */
public class MenuTheme {

  private static final MiniMessage MINI = MiniMessage.miniMessage();

  private final ArkonasRanksPlugin plugin;

  private final String primary;
  private final String secondary;
  private final String success;
  private final String danger;
  private final String muted;

  private final ItemStack borderBase;
  private final List<ItemStack> borderLights;
  private final int lights;

  private final ProgressBar progressBar;
  private final ConfigurationSection sounds;

  public MenuTheme(ArkonasRanksPlugin plugin, MenuConfig config) {
    this.plugin = plugin;
    ConfigurationSection theme = config.theme();

    this.primary = hex(theme, "primary", "#00E5FF");
    this.secondary = hex(theme, "secondary", "#7C4DFF");
    this.success = hex(theme, "success", "#3DDC84");
    this.danger = hex(theme, "danger", "#FF5370");
    this.muted = hex(theme, "muted", "#8A8F98");

    ConfigurationSection border = theme == null ? null : theme.getConfigurationSection("border");
    Material baseMaterial = material(border == null ? null : border.getString("base"),
        Material.BLACK_STAINED_GLASS_PANE);
    this.borderBase = pane(baseMaterial);

    this.borderLights = new ArrayList<>();
    List<String> lightNames = border == null ? List.of() : border.getStringList("light");
    for (String name : lightNames) {
      Material material = material(name, null);
      if (material != null) {
        borderLights.add(pane(material));
      }
    }
    if (borderLights.isEmpty()) {
      borderLights.add(pane(Material.CYAN_STAINED_GLASS_PANE));
    }
    int configuredLights = border == null ? borderLights.size() : border.getInt("lights", borderLights.size());
    this.lights = Math.max(1, Math.min(configuredLights, borderLights.size()));

    ConfigurationSection progress = theme == null ? null : theme.getConfigurationSection("progress");
    String filled = progress == null ? "▰" : progress.getString("filled-char", "▰");
    String empty = progress == null ? "▱" : progress.getString("empty-char", "▱");
    int barLength = progress == null ? 10 : progress.getInt("length", 10);
    this.progressBar = new ProgressBar(filled, empty, barLength);

    this.sounds = theme == null ? null : theme.getConfigurationSection("sounds");
  }

  public ProgressBar progressBar() {
    return progressBar;
  }

  public String primary() {
    return primary;
  }

  public String secondary() {
    return secondary;
  }

  public String success() {
    return success;
  }

  public String danger() {
    return danger;
  }

  public String muted() {
    return muted;
  }

  /** A MiniMessage gradient between the primary and secondary theme colours. */
  public Component gradientTitle(String text) {
    return MINI.deserialize("<gradient:" + primary + ":" + secondary + ">" + text + "</gradient>");
  }

  public int lights() {
    return lights;
  }

  /** The opaque base pane placed on every empty (non-functional) slot. */
  public ItemStack borderBase() {
    return borderBase;
  }

  /** The {@code i}-th chasing-light pane (wraps around the configured variants). */
  public ItemStack borderLight(int i) {
    return borderLights.get(Math.floorMod(i, borderLights.size()));
  }

  /**
   * Plays a configured UI sound. Silently no-ops if the sound key is unknown or
   * the platform (e.g. MockBukkit) cannot resolve it.
   */
  public void playSound(Player player, String key) {
    if (sounds == null || player == null) {
      return;
    }
    String name = sounds.getString(key, "");
    if (name.isEmpty()) {
      return;
    }
    try {
      Sound parsed = Registry.SOUNDS.get(
          NamespacedKey.minecraft(name.toLowerCase().replace(' ', '_')));
      if (parsed != null) {
        player.playSound(player.getLocation(), parsed, 0.6f, 1.2f);
      }
    } catch (Exception ignored) {
      // MockBukkit / unusual servers may not expose the sound registry
    }
  }

  private ItemStack pane(Material material) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
      item.setItemMeta(meta);
    }
    return item;
  }

  private static String hex(ConfigurationSection section, String key, String def) {
    if (section == null) {
      return def;
    }
    String value = section.getString(key, def);
    if (value == null || value.isEmpty()) {
      return def;
    }
    return value.startsWith("#") ? value : "#" + value;
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
