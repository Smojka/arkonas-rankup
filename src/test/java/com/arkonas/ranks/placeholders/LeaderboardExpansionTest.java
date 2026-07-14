package com.arkonas.ranks.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.data.StatsService;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** End-to-end: a real rankup surfaces through the leaderboard placeholders holograms read. */
class LeaderboardExpansionTest extends RankupTest {

  @Test
  void topPlaceholdersReflectLeaderboardAndFallBackWhenEmpty() {
    StatsService stats = plugin.getStats();
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 100000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getHelper().rankup(player);
    stats.flush(); // record() refreshes the leaderboard cache on the same thread

    RankupExpansion expansion = plugin.getPlaceholders().getExpansion();
    assertEquals(player.getName(), expansion.placeholder(player, "top_1_name"));
    assertEquals("1", expansion.placeholder(player, "top_1_count"));

    // an unfilled slot falls back to the configured empty text (name '', count '0')
    assertEquals("", expansion.placeholder(player, "top_2_name"));
    assertEquals("0", expansion.placeholder(player, "top_2_count"));
    // no prestige entries yet
    assertEquals("", expansion.placeholder(player, "prestige_top_1_name"));
  }
}
