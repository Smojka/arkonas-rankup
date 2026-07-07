package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.ConfirmScreen;
import com.arkonas.ranks.menu.screens.PrestigeListMenu;
import com.arkonas.ranks.menu.screens.PrestigeMenu;

public class PrestigeMenuTest extends RankupTest {

  public PrestigeMenuTest() {
    super("menusprestige");
  }

  @Test
  public void prestigeReadyWhenEligibleWithMoney() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "B");
    plugin.getEconomy().setPlayer(player, 100000);

    plugin.getMenuModule().openPrestige(player);
    PrestigeMenu menu = (PrestigeMenu) player.getOpenInventory().getTopInventory().getHolder();

    assertEquals(ConfirmScreen.State.READY, menu.getState());
    ItemStack confirm = menu.getInventory().getItem(menu.getConfirmSlot());
    assertEquals(Material.LIME_CONCRETE, confirm.getType());
  }

  @Test
  public void prestigeUnmetWithoutMoney() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "B");
    plugin.getEconomy().setPlayer(player, 0);

    plugin.getMenuModule().openPrestige(player);
    PrestigeMenu menu = (PrestigeMenu) player.getOpenInventory().getTopInventory().getHolder();

    assertEquals(ConfirmScreen.State.UNMET, menu.getState());
  }

  @Test
  public void prestigeListOpensAndCurrentClickOpensPrestige() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "B");
    plugin.getEconomy().setPlayer(player, 100000);

    plugin.getMenuModule().openPrestigeList(player);
    PrestigeListMenu list =
        (PrestigeListMenu) player.getOpenInventory().getTopInventory().getHolder();
    assertTrue(list.getCurrentSlot() >= 0, "the current prestige should be highlighted");

    player.simulateInventoryClick(list.getCurrentSlot());
    server.getScheduler().performTicks(1);

    InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
    assertTrue(holder instanceof PrestigeMenu, "clicking the current prestige opens the prestige menu");
  }
}
