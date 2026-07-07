package com.arkonas.ranks.data;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.arkonas.ranks.events.PlayerPrestigeEvent;
import com.arkonas.ranks.events.PlayerRankupEvent;
import com.arkonas.ranks.ranks.Rank;

/**
 * Captures rankup/prestige events into immutable records on the main thread
 * and submits them to the async stats writer.
 */
public class StatsListener implements Listener {

  private final StatsService stats;

  public StatsListener(StatsService stats) {
    this.stats = stats;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onRankup(PlayerRankupEvent event) {
    Rank rank = event.getRank().getRank();
    stats.record(new RankupRecord(event.getPlayer().getUniqueId(), event.getPlayer().getName(),
        RankupRecord.Type.RANKUP, rank.getRank(), rank.getNext(), System.currentTimeMillis()));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPrestige(PlayerPrestigeEvent event) {
    Rank prestige = event.getPrestige().getRank();
    stats.record(new RankupRecord(event.getPlayer().getUniqueId(), event.getPlayer().getName(),
        RankupRecord.Type.PRESTIGE, prestige.getRank(), prestige.getNext(),
        System.currentTimeMillis()));
  }
}
