package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.PrestigeMenu;
import com.arkonas.ranks.menu.screens.RankupMenu;

/**
 * The menus are an alternative front end for {@code /rankup} and {@code /prestige}, so they must be
 * gated by the same permissions. The hub is reachable from {@code /ranks}, and the rank path lets a
 * player click their current rank — without these checks either route ranks a player up while
 * {@code rankup.rankup} is revoked.
 */
public class MenuPermissionTest extends RankupTest {

  public MenuPermissionTest() {
    super("menus");
  }

  private PlayerMock playerAtA() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 500);
    return player;
  }

  /** The holder of whatever the player currently has open, or null when nothing opened. */
  private Object openHolder(PlayerMock player) {
    var view = player.getOpenInventory();
    return view == null || view.getTopInventory() == null
        ? null : view.getTopInventory().getHolder();
  }

  @Test
  public void rankupMenuDoesNotOpenWithoutTheRankupPermission() {
    PlayerMock player = playerAtA();
    player.addAttachment(plugin, MenuModule.PERM_RANKUP, false);

    plugin.getMenuModule().openRankup(player);

    assertFalse(openHolder(player) instanceof RankupMenu,
        "a player without rankup.rankup must not get the rankup screen");
  }

  @Test
  public void prestigeMenuDoesNotOpenWithoutThePrestigePermission() {
    PlayerMock player = playerAtA();
    player.addAttachment(plugin, MenuModule.PERM_PRESTIGE, false);

    plugin.getMenuModule().openPrestige(player);

    assertFalse(openHolder(player) instanceof PrestigeMenu,
        "a player without rankup.prestige must not get the prestige screen");
  }

  @Test
  public void confirmClickDoesNotRankUpWithoutTheRankupPermission() {
    PlayerMock player = playerAtA();

    // opened while allowed, then the permission is revoked before the click lands
    plugin.getMenuModule().openRankup(player);
    RankupMenu menu = (RankupMenu) openHolder(player);
    player.addAttachment(plugin, MenuModule.PERM_RANKUP, false);

    player.simulateInventoryClick(menu.getConfirmSlot());
    server.getScheduler().performTicks(2);

    assertTrue(groupProvider.inGroup(player.getUniqueId(), "A"),
        "the confirm must re-check the permission, not trust the state it opened with");
    assertEquals(500.0, plugin.getEconomy().getBalance(player),
        "a blocked rankup must not charge the player");
  }

  @Test
  public void confirmOnlyFiresOncePerScreen() {
    PlayerMock player = playerAtA();
    plugin.getMenuModule().openRankup(player);
    RankupMenu menu = (RankupMenu) openHolder(player);

    // several click packets can arrive in one tick, before the deferred confirm has run
    player.simulateInventoryClick(menu.getConfirmSlot());
    player.simulateInventoryClick(menu.getConfirmSlot());
    player.simulateInventoryClick(menu.getConfirmSlot());
    server.getScheduler().performTicks(2);

    assertTrue(groupProvider.inGroup(player.getUniqueId(), "B"), "one click ranks up once");
    assertEquals(400.0, plugin.getEconomy().getBalance(player),
        "a single confirm screen may only charge one rankup");
  }
}
