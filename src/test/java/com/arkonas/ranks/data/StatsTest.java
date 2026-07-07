package com.arkonas.ranks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;

public class StatsTest extends RankupTest {

  @Test
  public void testRankupWritesHistoryAndLeaderboard() throws Exception {
    StatsService stats = plugin.getStats();
    assertNotNull(stats, "stats service should be enabled by default");

    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 100000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getHelper().rankup(player);
    stats.flush();

    CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();
    stats.top(false, 10, future::complete);
    List<LeaderboardEntry> top = future.get();

    assertEquals(1, top.size());
    assertEquals(player.getName(), top.get(0).name());
    assertEquals(1, top.get(0).count());
    assertEquals(player.getUniqueId(), top.get(0).uuid());
  }

  @Test
  public void testCachedCountsAfterRankup() throws Exception {
    StatsService stats = plugin.getStats();
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 100000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getHelper().rankup(player);
    stats.flush();

    stats.cachedCounts(player.getUniqueId()); // triggers async load
    stats.flush();
    assertEquals(1, stats.cachedCounts(player.getUniqueId())[0]);
  }
}
