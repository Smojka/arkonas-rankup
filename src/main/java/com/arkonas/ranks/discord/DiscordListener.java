package com.arkonas.ranks.discord;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.arkonas.ranks.events.PlayerPrestigeEvent;
import com.arkonas.ranks.events.PlayerRankupEvent;
import com.arkonas.ranks.prestige.Prestige;
import com.arkonas.ranks.ranks.Rank;

/**
 * Forwards the plugin's own rankup/prestige events to {@link DiscordAnnouncer} at MONITOR priority,
 * so manual rankups, /maxrankup, autorankup and forced rankups all announce without touching the
 * core rankup flow — the same seam {@code EffectsListener} and {@code StatsListener} use.
 */
public class DiscordListener implements Listener {

  private final DiscordAnnouncer announcer;

  public DiscordListener(DiscordAnnouncer announcer) {
    this.announcer = announcer;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onRankup(PlayerRankupEvent event) {
    Rank rank = event.getRank().getRank();
    announcer.announceRankup(event.getPlayer().getName(), fromLabel(rank), rank.getNext());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPrestige(PlayerPrestigeEvent event) {
    Rank prestige = event.getPrestige().getRank();
    announcer.announcePrestige(event.getPlayer().getName(), fromLabel(prestige), prestige.getNext());
  }

  /**
   * The "from" label. A rank's group name is used directly, but the root prestige has a null group
   * name (that is how the first prestige is detected), so fall back to its base-rank origin to avoid
   * an empty {@code %from%} on a player's first prestige.
   */
  private static String fromLabel(Rank rank) {
    if (rank.getRank() != null) {
      return rank.getRank();
    }
    return rank instanceof Prestige prestige ? prestige.getFrom() : "";
  }
}
