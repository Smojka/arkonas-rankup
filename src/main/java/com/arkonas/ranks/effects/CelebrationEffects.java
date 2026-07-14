package com.arkonas.ranks.effects;

import java.time.Duration;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import com.arkonas.ranks.ArkonasRanksPlugin;

/**
 * Plays a configured celebration (sound, particles, firework, title, action bar,
 * boss bar) for a single rankup or prestige. All Bukkit calls happen on the main
 * thread; callers are the MONITOR event listeners in {@link EffectsListener}.
 */
public class CelebrationEffects {

  public static final NamespacedKey FIREWORK_KEY =
      new NamespacedKey("arkonasranks", "celebration_firework");

  private final ArkonasRanksPlugin plugin;

  public CelebrationEffects(ArkonasRanksPlugin plugin) {
    this.plugin = plugin;
  }

  public void play(Player player, ConfigurationSection section,
      String oldRank, String newRank) {
    if (section == null) {
      return;
    }
    playSound(player, section.getConfigurationSection("sound"));
    playJingle(player, section.getConfigurationSection("jingle"));
    playParticle(player, section.getConfigurationSection("particle"));
    playFirework(player, section.getConfigurationSection("firework"));

    List<Player> audience = audience(player, section.getString("broadcast-mode", "none"));
    showTitle(player, audience, section.getConfigurationSection("title"), oldRank, newRank);
    showActionBar(player, section.getString("actionbar", ""), oldRank, newRank);
    showBossBar(player, audience, section.getConfigurationSection("bossbar"), oldRank, newRank);
  }

  private List<Player> audience(Player player, String mode) {
    return switch (mode == null ? "none" : mode.toLowerCase()) {
      case "server" -> List.copyOf(plugin.getServer().getOnlinePlayers());
      case "world" -> List.copyOf(player.getWorld().getPlayers());
      default -> List.of(player);
    };
  }

  private Component render(Player player, String message, String oldRank, String newRank) {
    String replaced = message
        .replace("%player%", player.getName())
        .replace("%rank%", oldRank == null ? "" : oldRank)
        .replace("%next%", newRank == null ? "" : newRank);
    return plugin.getComponentRenderer().render(replaced);
  }

  private void playSound(Player player, ConfigurationSection sound) {
    if (sound == null) {
      return;
    }
    String name = sound.getString("name", "");
    if (name.isEmpty()) {
      return;
    }
    Sound parsed = Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase().replace(' ', '_')));
    if (parsed == null) {
      plugin.getLogger().warning("Unknown celebration sound: " + name);
      return;
    }
    player.playSound(player.getLocation(), parsed,
        (float) sound.getDouble("volume", 1.0), (float) sound.getDouble("pitch", 1.0));
  }

  /**
   * Plays a short ascending "jingle" — the same note-block sound repeated with a rising pitch on a
   * fixed tick interval. Scheduled on the main thread (one task per note); pitch is clamped to the
   * musical note-block range [0.5, 2.0]. Opt-in via {@code jingle.enabled}.
   */
  private void playJingle(Player player, ConfigurationSection jingle) {
    if (jingle == null || !jingle.getBoolean("enabled", false)) {
      return;
    }
    String name = jingle.getString("sound", "block.note_block.pling");
    Sound parsed;
    try {
      parsed = Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase().replace(' ', '_')));
    } catch (Exception e) {
      parsed = null;
    }
    if (parsed == null) {
      plugin.getLogger().warning("Unknown jingle sound: " + name);
      return;
    }
    final Sound sound = parsed;
    int notes = Math.max(1, jingle.getInt("notes", 5));
    double startPitch = jingle.getDouble("start-pitch", 0.8);
    double pitchStep = jingle.getDouble("pitch-step", 0.15);
    float volume = (float) jingle.getDouble("volume", 1.0);
    long interval = Math.max(1, jingle.getLong("interval-ticks", 3));

    for (int i = 0; i < notes; i++) {
      float pitch = (float) Math.max(0.5, Math.min(2.0, startPitch + i * pitchStep));
      long delay = (long) i * interval;
      plugin.getServer().getScheduler().runTaskLater(plugin,
          () -> player.playSound(player.getLocation(), sound, volume, pitch), delay);
    }
  }

  private void playParticle(Player player, ConfigurationSection particle) {
    if (particle == null) {
      return;
    }
    String type = particle.getString("type", "");
    if (type.isEmpty()) {
      return;
    }
    Particle parsed;
    try {
      parsed = Particle.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      plugin.getLogger().warning("Unknown celebration particle: " + type);
      return;
    }
    double offset = particle.getDouble("offset", 0.5);
    player.getWorld().spawnParticle(parsed, player.getLocation().add(0, 1, 0),
        particle.getInt("count", 30), offset, offset, offset, 0);
  }

  private void playFirework(Player player, ConfigurationSection firework) {
    if (firework == null || !firework.getBoolean("enabled", false)) {
      return;
    }
    FireworkEffect.Type type;
    try {
      type = FireworkEffect.Type.valueOf(firework.getString("type", "BALL").toUpperCase());
    } catch (IllegalArgumentException e) {
      type = FireworkEffect.Type.BALL;
    }
    FireworkEffect.Builder effect = FireworkEffect.builder().with(type).flicker(true).trail(true);
    for (String hex : firework.getStringList("colors")) {
      try {
        effect.withColor(Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16)));
      } catch (NumberFormatException e) {
        plugin.getLogger().warning("Invalid firework color: " + hex);
      }
    }
    FireworkEffect built = effect.build();
    int power = Math.max(0, Math.min(firework.getInt("power", 1), 2));
    player.getWorld().spawn(player.getLocation(), Firework.class, fw -> {
      FireworkMeta meta = fw.getFireworkMeta();
      meta.addEffect(built);
      meta.setPower(power);
      fw.setFireworkMeta(meta);
      // tagged so EffectsListener cancels damage from celebration fireworks
      fw.getPersistentDataContainer().set(FIREWORK_KEY, PersistentDataType.BYTE, (byte) 1);
    });
  }

  private void showTitle(Player player, List<Player> audience, ConfigurationSection title,
      String oldRank, String newRank) {
    if (title == null) {
      return;
    }
    String main = title.getString("title", "");
    String subtitle = title.getString("subtitle", "");
    if (main.isEmpty() && subtitle.isEmpty()) {
      return;
    }
    Title.Times times = Title.Times.times(
        Duration.ofMillis(title.getInt("fade-in", 10) * 50L),
        Duration.ofMillis(title.getInt("stay", 60) * 50L),
        Duration.ofMillis(title.getInt("fade-out", 20) * 50L));
    Title rendered = Title.title(
        render(player, main, oldRank, newRank),
        render(player, subtitle, oldRank, newRank), times);
    for (Player viewer : audience) {
      viewer.showTitle(rendered);
    }
  }

  private void showActionBar(Player player, String actionbar, String oldRank, String newRank) {
    if (actionbar == null || actionbar.isEmpty()) {
      return;
    }
    player.sendActionBar(render(player, actionbar, oldRank, newRank));
  }

  private void showBossBar(Player player, List<Player> audience, ConfigurationSection bossbar,
      String oldRank, String newRank) {
    if (bossbar == null || !bossbar.getBoolean("enabled", false)) {
      return;
    }
    BossBar.Color color;
    try {
      color = BossBar.Color.valueOf(bossbar.getString("color", "GREEN").toUpperCase());
    } catch (IllegalArgumentException e) {
      color = BossBar.Color.GREEN;
    }
    BossBar.Overlay overlay;
    try {
      overlay = BossBar.Overlay.valueOf(bossbar.getString("overlay", "PROGRESS").toUpperCase());
    } catch (IllegalArgumentException e) {
      overlay = BossBar.Overlay.PROGRESS;
    }
    BossBar bar = BossBar.bossBar(
        render(player, bossbar.getString("text", ""), oldRank, newRank), 1f, color, overlay);
    for (Player viewer : audience) {
      viewer.showBossBar(bar);
    }
    long ticks = bossbar.getInt("seconds", 5) * 20L;
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      for (Player viewer : audience) {
        viewer.hideBossBar(bar);
      }
    }, ticks);
  }
}
