package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.data.RankupRecord;
import com.arkonas.ranks.data.StatsService;
import com.arkonas.ranks.menu.screens.LeaderboardMenu;

public class LeaderboardMenuTest extends RankupTest {

  public LeaderboardMenuTest() {
    super("menus");
  }

  private LeaderboardMenu openLoaded(PlayerMock viewer, boolean prestiges) {
    viewer.addAttachment(plugin, "rankup.top", true);
    plugin.getMenuModule().openLeaderboard(viewer, prestiges);
    LeaderboardMenu menu = (LeaderboardMenu) viewer.getOpenInventory().getTopInventory().getHolder();
    plugin.getStats().flush();            // let the async top() query finish
    server.getScheduler().performTicks(2); // run the main-thread refresh
    return menu;
  }

  @Test
  public void podiumHeadsAndCounts() {
    StatsService stats = plugin.getStats();
    UUID alpha = UUID.randomUUID();
    UUID beta = UUID.randomUUID();
    long now = System.currentTimeMillis();
    stats.record(new RankupRecord(alpha, "Alpha", RankupRecord.Type.RANKUP, "A", "B", now));
    stats.record(new RankupRecord(alpha, "Alpha", RankupRecord.Type.RANKUP, "B", "C", now));
    stats.record(new RankupRecord(beta, "Beta", RankupRecord.Type.RANKUP, "A", "B", now));
    stats.flush();

    PlayerMock viewer = server.addPlayer();
    LeaderboardMenu menu = openLoaded(viewer, false);

    assertTrue(menu.isLoaded(), "leaderboard should have loaded");
    assertEquals(2, menu.getEntries().size());
    assertEquals("Alpha", menu.getEntries().get(0).name());
    assertEquals(2, menu.getEntries().get(0).count());
    assertEquals(Material.PLAYER_HEAD, menu.getInventory().getItem(13).getType(),
        "the #1 podium slot should hold a player head");
  }

  @Test
  public void toggleSwitchesToPrestiges() {
    PlayerMock viewer = server.addPlayer();
    LeaderboardMenu menu = openLoaded(viewer, false);

    viewer.simulateInventoryClick(menu.getToggleSlot());
    server.getScheduler().performTicks(1);

    LeaderboardMenu toggled =
        (LeaderboardMenu) viewer.getOpenInventory().getTopInventory().getHolder();
    assertTrue(toggled.isPrestiges(), "the toggle should switch to the prestige leaderboard");
  }
}
