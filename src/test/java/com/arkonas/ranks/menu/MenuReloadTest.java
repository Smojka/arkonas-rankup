package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;

public class MenuReloadTest extends RankupTest {

  public MenuReloadTest() {
    super("menus");
  }

  @Test
  public void reloadClosesOpenMenusAndStopsTicker() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getMenuModule().openHub(player);
    server.getScheduler().performTicks(3);

    assertFalse(plugin.getMenuModule().getOpenMenus().isEmpty(), "hub should be open");
    assertTrue(player.getOpenInventory().getTopInventory().getHolder()
        instanceof com.arkonas.ranks.menu.AbstractMenu);

    plugin.reload(false);

    assertTrue(plugin.getMenuModule().getOpenMenus().isEmpty(),
        "reload must forget all open menus (no ticker leak)");
    org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
    assertFalse(top != null && top.getHolder() instanceof com.arkonas.ranks.menu.AbstractMenu,
        "the menu should be closed");

    // ticker must not keep animating after a reload
    server.getScheduler().performTicks(5);
    assertTrue(plugin.getMenuModule().getOpenMenus().isEmpty());
  }
}
