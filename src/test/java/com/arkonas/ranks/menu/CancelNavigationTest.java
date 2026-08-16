package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.ConfirmScreen;
import com.arkonas.ranks.menu.screens.RankPathMenu;
import com.arkonas.ranks.menu.screens.RankupMenu;

public class CancelNavigationTest extends RankupTest {

  public CancelNavigationTest() {
    super("menus");
  }

  @Test
  public void pathToRankupAndBackReturnsToPath() {
    PlayerMock player = server.addPlayer();
    // the path opens with rankup.ranks; clicking the current rank also needs rankup.rankup
    player.addAttachment(plugin, "rankup.ranks", true);
    player.addAttachment(plugin, "rankup.rankup", true);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 500);

    plugin.getMenuModule().openRankPath(player);
    RankPathMenu path = (RankPathMenu) player.getOpenInventory().getTopInventory().getHolder();
    assertTrue(path.getCurrentSlot() >= 0, "the current rank should be on the path");

    // click the current rank -> opens the rankup menu with the path as parent
    player.simulateInventoryClick(path.getCurrentSlot());
    server.getScheduler().performTicks(1);

    InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
    assertTrue(holder instanceof RankupMenu, "clicking the current rank opens the rankup menu");
    RankupMenu rankup = (RankupMenu) holder;
    assertEquals(ConfirmScreen.State.READY, rankup.getState());

    // back -> the path menu (the screen's own cancel button is gone; the nav bar is the way out)
    player.simulateInventoryClick(rankup.getBackSlot());
    server.getScheduler().performTicks(1);

    assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof RankPathMenu,
        "back should return to the rank path menu");
  }
}
