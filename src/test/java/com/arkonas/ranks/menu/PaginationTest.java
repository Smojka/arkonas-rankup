package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.RankPathMenu;

public class PaginationTest extends RankupTest {

  public PaginationTest() {
    super("menuslongladder");
  }

  @Test
  public void firstPageIsFullAndPagesToTheSecond() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.ranks", true);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getMenuModule().openRankPath(player);
    RankPathMenu page0 = (RankPathMenu) player.getOpenInventory().getTopInventory().getHolder();

    assertEquals(0, page0.getPage());
    assertTrue(page0.getTotalPages() >= 2, "the long ladder must span multiple pages");
    assertEquals(28, page0.getPlacedEntries(), "the first page holds a full 7x4 grid");
    assertNotNull(page0.getInventory().getItem(53), "the next-page arrow should be present");

    player.simulateInventoryClick(53); // next-page arrow
    server.getScheduler().performTicks(1);

    RankPathMenu page1 = (RankPathMenu) player.getOpenInventory().getTopInventory().getHolder();
    assertEquals(1, page1.getPage(), "clicking next should open page 2");
    assertTrue(page1.getPlacedEntries() > 0 && page1.getPlacedEntries() <= 28);
  }
}
