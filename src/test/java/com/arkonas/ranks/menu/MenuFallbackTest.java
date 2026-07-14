package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.gui.Gui;
import com.arkonas.ranks.menu.screens.RankPathMenu;
import com.arkonas.ranks.menu.screens.RankupMenu;

/**
 * With menus.enabled: false the plugin must behave exactly like the classic
 * chat-based core: the confirmation GUI opens for /rankup and /ranks writes to chat.
 */
public class MenuFallbackTest extends RankupTest {

  public MenuFallbackTest() {
    super("menusdisabled");
  }

  @Test
  public void menuModuleNotConstructed() {
    assertNull(plugin.getMenuModule(), "menu module must not be built when menus.enabled is false");
  }

  @Test
  public void rankupOpensClassicGui() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.rankup", true);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 10000);

    server.dispatchCommand(player, "rankup");

    Inventory top = player.getOpenInventory().getTopInventory();
    assertTrue(top.getHolder() instanceof Gui, "expected the classic confirmation Gui");
    assertFalse(top.getHolder() instanceof RankupMenu);
  }

  @Test
  public void ranksStaysChat() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.ranks", true);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    server.dispatchCommand(player, "ranks");

    Inventory top = player.getOpenInventory().getTopInventory();
    assertFalse(top != null && top.getHolder() instanceof RankPathMenu);
    assertNotNull(player.nextMessage(), "expected the classic chat rank listing");
  }
}
