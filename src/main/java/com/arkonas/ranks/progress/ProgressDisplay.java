package com.arkonas.ranks.progress;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.requirements.Requirement;

/**
 * Always-on progress feedback toward a player's next rank: an optional persistent boss bar, an
 * action-bar line and an experience-bar mirror, refreshed on a fixed interval. All are opt-in and
 * off by default. The exp-bar mirror overwrites the vanilla XP bar fill (not the level, which the
 * {@code xp-level} requirement reads).
 */
public final class ProgressDisplay extends BukkitRunnable implements Listener {

  private final ArkonasRanksPlugin plugin;
  private final boolean expBar;
  private final boolean bossBarEnabled;
  private final String bossBarText;
  private final BossBar.Color bossBarColour;
  private final BossBar.Overlay bossBarOverlay;
  private final boolean actionBarEnabled;
  private final String actionBarText;
  private final boolean scoreboardEnabled;
  private final String scoreboardTitle;
  private final java.util.List<String> scoreboardLines;

  private final Map<UUID, BossBar> bossBars = new HashMap<>();
  private final Map<UUID, org.bukkit.scoreboard.Scoreboard> boards = new HashMap<>();

  private ProgressDisplay(ArkonasRanksPlugin plugin, boolean expBar, boolean bossBarEnabled,
      String bossBarText, BossBar.Color bossBarColour, BossBar.Overlay bossBarOverlay,
      boolean actionBarEnabled, String actionBarText, boolean scoreboardEnabled,
      String scoreboardTitle, java.util.List<String> scoreboardLines) {
    this.plugin = plugin;
    this.expBar = expBar;
    this.bossBarEnabled = bossBarEnabled;
    this.bossBarText = bossBarText;
    this.bossBarColour = bossBarColour;
    this.bossBarOverlay = bossBarOverlay;
    this.actionBarEnabled = actionBarEnabled;
    this.actionBarText = actionBarText;
    this.scoreboardEnabled = scoreboardEnabled;
    this.scoreboardTitle = scoreboardTitle;
    this.scoreboardLines = scoreboardLines;
  }

  /** Builds a display from config, or null when the feature is disabled. */
  public static ProgressDisplay fromConfig(ArkonasRanksPlugin plugin, ConfigurationSection section) {
    if (section == null || !section.getBoolean("enabled", false)) {
      return null;
    }
    ConfigurationSection boss = section.getConfigurationSection("bossbar");
    ConfigurationSection action = section.getConfigurationSection("actionbar");
    ConfigurationSection board = section.getConfigurationSection("scoreboard");
    java.util.List<String> lines = board == null ? java.util.List.of()
        : board.getStringList("lines");
    if (lines.isEmpty()) {
      lines = java.util.List.of("&fRank: &e%rank%", "&fNext: &e%next%", "&a%bar% &7%percent%%");
    }
    return new ProgressDisplay(plugin,
        section.getBoolean("expbar", false),
        boss != null && boss.getBoolean("enabled", false),
        boss == null ? "&aNext: %next% &7- &e%percent%%"
            : boss.getString("text", "&aNext: %next% &7- &e%percent%%"),
        colour(boss == null ? null : boss.getString("color"), BossBar.Color.GREEN),
        overlay(boss == null ? null : boss.getString("overlay"), BossBar.Overlay.PROGRESS),
        action != null && action.getBoolean("enabled", false),
        action == null ? "&7Progress to %next%: &e%percent%%"
            : action.getString("text", "&7Progress to %next%: &e%percent%%"),
        board != null && board.getBoolean("enabled", false),
        board == null ? "&6&lRankUp" : board.getString("title", "&6&lRankUp"),
        lines);
  }

  public long intervalTicks(ConfigurationSection section) {
    return Math.max(1, section == null ? 20 : section.getLong("interval-ticks", 20));
  }

  @Override
  public void run() {
    if (plugin.error()) {
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      update(player);
    }
  }

  /** Refreshes every enabled display for one player. */
  public void update(Player player) {
    double fraction = fractionToNext(player);
    float progress = (float) Math.max(0, Math.min(1, fraction));

    if (expBar) {
      player.setExp(progress);
    }
    if (bossBarEnabled) {
      BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(),
          id -> BossBar.bossBar(Component.empty(), progress, bossBarColour, bossBarOverlay));
      bar.progress(progress);
      bar.name(render(player, bossBarText, fraction));
      player.showBossBar(bar);
    }
    if (actionBarEnabled) {
      player.sendActionBar(render(player, actionBarText, fraction));
    }
    if (scoreboardEnabled) {
      updateScoreboard(player, fraction);
    }
  }

  private void updateScoreboard(Player player, double fraction) {
    org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
    if (manager == null) {
      return;
    }
    org.bukkit.scoreboard.Scoreboard board =
        boards.computeIfAbsent(player.getUniqueId(), id -> manager.getNewScoreboard());
    org.bukkit.scoreboard.Objective objective = board.getObjective("arprogress");
    if (objective == null) {
      objective = board.registerNewObjective("arprogress", "dummy", legacy(scoreboardTitle));
      objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
    }
    for (String entry : board.getEntries()) {
      board.resetScores(entry);
    }
    int percent = (int) Math.round(Math.max(0, Math.min(1, fraction)) * 100);
    String bar = progressBar(fraction, 10, '■', '□');
    int score = scoreboardLines.size();
    for (int i = 0; i < scoreboardLines.size(); i++) {
      String line = legacy(applyPlaceholders(scoreboardLines.get(i), player.getName(),
          rankName(player), nextName(player), percent, bar));
      // append i invisible reset codes so otherwise-identical lines stay distinct scoreboard entries
      objective.getScore(line + "§r".repeat(i)).setScore(score--);
    }
    if (player.getScoreboard() != board) {
      player.setScoreboard(board);
    }
  }

  /** Substitutes the scoreboard/line placeholders. Pure; unit-tested. */
  static String applyPlaceholders(String template, String player, String rank, String next,
      int percent, String bar) {
    return template
        .replace("%player%", player)
        .replace("%rank%", rank)
        .replace("%next%", next)
        .replace("%percent%", String.valueOf(percent))
        .replace("%bar%", bar);
  }

  /** A fixed-width progress bar of filled/empty characters for the given fraction. Pure. */
  static String progressBar(double fraction, int length, char filled, char empty) {
    int on = (int) Math.round(Math.max(0, Math.min(1, fraction)) * length);
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(i < on ? filled : empty);
    }
    return builder.toString();
  }

  private static String legacy(String text) {
    return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
  }

  /**
   * Average completion across the current rank's requirements, in [0, 1]. Returns 1.0 when the
   * player is not in a ladder or is at the top rank (nothing left to progress toward).
   */
  public double fractionToNext(Player player) {
    RankElement<Rank> element = plugin.getRankups().getByPlayer(player);
    if (element == null || !element.hasNext()) {
      return 1.0;
    }
    double sum = 0;
    int count = 0;
    for (Requirement requirement : element.getRank().getRequirements().getRequirements(player)) {
      double total = requirement.getTotal(player);
      double done = total <= 0 ? 1.0
          : Math.max(0, Math.min(1, (total - requirement.getRemaining(player)) / total));
      sum += done;
      count++;
    }
    return count == 0 ? 1.0 : sum / count;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    clear(event.getPlayer());
  }

  /** Hides and forgets a player's boss bar and sidebar (on quit / disable). */
  public void clear(Player player) {
    BossBar bar = bossBars.remove(player.getUniqueId());
    if (bar != null) {
      player.hideBossBar(bar);
    }
    if (boards.remove(player.getUniqueId()) != null) {
      org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
      if (manager != null && player.isOnline()) {
        player.setScoreboard(manager.getMainScoreboard());
      }
    }
  }

  private Component render(Player player, String template, double fraction) {
    String replaced = template
        .replace("%player%", player.getName())
        .replace("%percent%", String.valueOf((int) Math.round(fraction * 100)))
        .replace("%rank%", rankName(player))
        .replace("%next%", nextName(player));
    return plugin.getComponentRenderer().render(replaced);
  }

  private String rankName(Player player) {
    RankElement<Rank> element = plugin.getRankups().getByPlayer(player);
    return element == null ? "" : String.valueOf(element.getRank().getRank());
  }

  private String nextName(Player player) {
    RankElement<Rank> element = plugin.getRankups().getByPlayer(player);
    return element == null || !element.hasNext() ? ""
        : String.valueOf(element.getNext().getRank().getRank());
  }

  private static BossBar.Color colour(String name, BossBar.Color def) {
    if (name == null) {
      return def;
    }
    try {
      return BossBar.Color.valueOf(name.toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return def;
    }
  }

  private static BossBar.Overlay overlay(String name, BossBar.Overlay def) {
    if (name == null) {
      return def;
    }
    try {
      return BossBar.Overlay.valueOf(name.toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return def;
    }
  }
}
