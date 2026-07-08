package com.arkonas.ranks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
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

  @Test
  public void milestoneHookReceivesFreshCount() {
    StatsService stats = plugin.getStats();
    AtomicReference<int[]> counts = new AtomicReference<>();
    AtomicReference<RankupRecord> record = new AtomicReference<>();
    stats.setMilestoneHook((rec, rankupCount, prestigeCount) -> {
      record.set(rec);
      counts.set(new int[]{rankupCount, prestigeCount});
    });

    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 100000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getHelper().rankup(player);
    stats.flush();

    // the hook runs after the write on the same stats thread, so it sees the new total
    assertArrayEquals(new int[]{1, 0}, counts.get());
    assertEquals(player.getUniqueId(), record.get().uuid());
  }
}
