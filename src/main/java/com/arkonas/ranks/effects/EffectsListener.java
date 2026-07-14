package com.arkonas.ranks.effects;

import java.io.File;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.events.PlayerPrestigeEvent;
import com.arkonas.ranks.events.PlayerRankupEvent;
import com.arkonas.ranks.ranks.Rank;

/**
 * Additive celebration module. Listens at MONITOR priority to the plugin's own
 * events so manual rankups, /maxrankup, autorankup and forced rankups are all
 * covered without touching the core rankup flow.
 */
public class EffectsListener implements Listener {

  private final ArkonasRanksPlugin plugin;
  private final CelebrationEffects effects;
  private YamlConfiguration config;

  public EffectsListener(ArkonasRanksPlugin plugin) {
    this.plugin = plugin;
    this.effects = new CelebrationEffects(plugin);
    reload();
  }

  public final void reload() {
    File file = new File(plugin.getDataFolder(), "effects.yml");
    if (!file.exists()) {
      plugin.saveResource("effects.yml", false);
    }
    config = YamlConfiguration.loadConfiguration(file);
  }

  private boolean enabled() {
    return config.getBoolean("enabled", true);
  }

  /**
   * Per-rank "celebration:" section in rankups.yml/prestiges.yml wins over the
   * global effects.yml section.
   */
  private ConfigurationSection section(Rank rank, String globalKey) {
    if (rank != null && rank.getSection() != null) {
      ConfigurationSection override = rank.getSection().getConfigurationSection("celebration");
      if (override != null) {
        return override;
      }
    }
    return config.getConfigurationSection(globalKey);
  }

  /**
   * Plays the {@code rebirth} celebration section for a rebirth. Rebirth fires no rankup/prestige
   * event, so {@link com.arkonas.ranks.rebirth.RebirthManager} calls this directly. A distinct
   * section lets servers give rebirths their own stinger.
   */
  public void celebrateRebirth(org.bukkit.entity.Player player, String from, String to) {
    if (!enabled()) {
      return;
    }
    effects.play(player, config.getConfigurationSection("rebirth"), from, to);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onRankup(PlayerRankupEvent event) {
    if (!enabled()) {
      return;
    }
    Rank rank = event.getRank().getRank();
    effects.play(event.getPlayer(), section(rank, "rankup"), rank.getRank(), rank.getNext());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPrestige(PlayerPrestigeEvent event) {
    if (!enabled()) {
      return;
    }
    Rank prestige = event.getPrestige().getRank();
    effects.play(event.getPlayer(), section(prestige, "prestige"),
        prestige.getRank(), prestige.getNext());
  }

  @EventHandler(ignoreCancelled = true)
  public void onFireworkDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Firework firework
        && firework.getPersistentDataContainer()
            .has(CelebrationEffects.FIREWORK_KEY, PersistentDataType.BYTE)) {
      event.setCancelled(true);
    }
  }
}
