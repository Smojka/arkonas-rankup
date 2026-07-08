package com.arkonas.ranks.discord;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.arkonas.ranks.events.PlayerPrestigeEvent;
import com.arkonas.ranks.events.PlayerRankupEvent;
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
    announcer.announceRankup(event.getPlayer().getName(), rank.getRank(), rank.getNext());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPrestige(PlayerPrestigeEvent event) {
    Rank prestige = event.getPrestige().getRank();
    announcer.announcePrestige(event.getPlayer().getName(), prestige.getRank(), prestige.getNext());
  }
}
