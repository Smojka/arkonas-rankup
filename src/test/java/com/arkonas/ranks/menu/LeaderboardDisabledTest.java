package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.LeaderboardMenu;

/**
 * With database.enabled: false the stats service is null, so the leaderboard
 * must show a disabled barrier rather than trying to load anything.
 */
public class LeaderboardDisabledTest extends RankupTest {

  public LeaderboardDisabledTest() {
    super("menusnostats");
  }

  @Test
  public void statsDisabledShowsBarrier() {
    assertNull(plugin.getStats(), "stats should be disabled by the fixture");

    PlayerMock viewer = server.addPlayer();
    plugin.getMenuModule().openLeaderboard(viewer, false);
    LeaderboardMenu menu = (LeaderboardMenu) viewer.getOpenInventory().getTopInventory().getHolder();

    assertEquals(Material.BARRIER, menu.getInventory().getItem(22).getType(),
        "a disabled-stats barrier should be shown");
  }
}
