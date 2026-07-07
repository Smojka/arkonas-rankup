package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.ConfirmScreen;
import com.arkonas.ranks.menu.screens.RankupMenu;

public class ConfirmClickTest extends RankupTest {

  public ConfirmClickTest() {
    super("menus");
  }

  private RankupMenu openRankup(PlayerMock player) {
    plugin.getMenuModule().openRankup(player);
    return (RankupMenu) player.getOpenInventory().getTopInventory().getHolder();
  }

  @Test
  public void confirmClickRanksUpAndCloses() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.rankup", true);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 500);

    RankupMenu menu = openRankup(player);
    assertEquals(ConfirmScreen.State.READY, menu.getState());

    player.simulateInventoryClick(menu.getConfirmSlot());
    server.getScheduler().performTicks(2); // run the deferred close + rankup

    assertTrue(plugin.getPermissions().inGroup(player.getUniqueId(), "B"),
        "confirm should rank the player up to B");
    assertEquals(400, plugin.getEconomy().getBalance(player), 0.01, "money should be deducted");
    assertFalse(plugin.getMenuModule().getOpenMenus().contains(menu),
        "the menu should be closed after confirming");
  }

  @Test
  public void confirmRechecksRequirements() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.rankup", true);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 500);

    RankupMenu menu = openRankup(player);
    assertEquals(ConfirmScreen.State.READY, menu.getState());

    // the money is removed *after* the menu was opened in the READY state
    plugin.getEconomy().setPlayer(player, 0);

    player.simulateInventoryClick(menu.getConfirmSlot());
    server.getScheduler().performTicks(2);

    assertFalse(plugin.getPermissions().inGroup(player.getUniqueId(), "B"),
        "the confirm click must re-check requirements and refuse the rankup");
  }
}
