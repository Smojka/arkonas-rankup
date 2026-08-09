package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.ConfirmScreen;
import com.arkonas.ranks.menu.screens.RankupMenu;

public class RequirementStateTest extends RankupTest {

  public RequirementStateTest() {
    super("menus");
  }

  private RankupMenu openRankup(PlayerMock player) {
    player.addAttachment(plugin, "rankup.rankup", true);
    plugin.getMenuModule().openRankup(player);
    return (RankupMenu) player.getOpenInventory().getTopInventory().getHolder();
  }

  @Test
  public void unmetShowsMoneyIconUnmet() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 0);

    RankupMenu menu = openRankup(player);

    assertEquals(ConfirmScreen.State.UNMET, menu.getState());
    assertTrue(hasMaterial(menu.getInventory(), Material.GOLD_INGOT),
        "the money requirement should be shown as a GOLD_INGOT");
    assertTrue(menu.getConfirmSlot() < 0, "there must be no confirm button while unmet");
  }

  @Test
  public void metShowsConfirmButton() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 500);

    RankupMenu menu = openRankup(player);

    assertEquals(ConfirmScreen.State.READY, menu.getState());
    ItemStack confirm = menu.getInventory().getItem(menu.getConfirmSlot());
    assertEquals(Material.LIME_CONCRETE, confirm.getType(), "expected the confirm button");
  }

  private static boolean hasMaterial(Inventory inventory, Material material) {
    for (ItemStack item : inventory.getContents()) {
      if (item != null && item.getType() == material) {
        return true;
      }
    }
    return false;
  }
}
