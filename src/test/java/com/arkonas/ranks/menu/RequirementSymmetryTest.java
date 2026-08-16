package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.ConfirmScreen;
import com.arkonas.ranks.menu.screens.RankupMenu;

/**
 * The requirement icons are mirrored around the centre column of their row. An even number of
 * them used to start one slot left of centre, which is what made the grid look lopsided.
 */
public class RequirementSymmetryTest extends RankupTest {

  /** The interior columns of the requirement row on a 5-row menu (slot 18 and 26 are border). */
  private static final int ROW_FROM = 19;
  private static final int ROW_TO = 25;

  public RequirementSymmetryTest() {
    super("menussymmetry"); // rank A carries four requirements, rank B three
  }

  private RankupMenu openRankup(PlayerMock player) {
    player.addAttachment(plugin, "rankup.rankup", true);
    plugin.getMenuModule().openRankup(player);
    return (RankupMenu) player.getOpenInventory().getTopInventory().getHolder();
  }

  @Test
  public void fourRequirementsStraddleTheCentreColumn() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 0);

    RankupMenu menu = openRankup(player);
    assertEquals(ConfirmScreen.State.UNMET, menu.getState());

    // two icons each side of slot 22, none on the centre itself
    assertEquals(List.of(20, 21, 23, 24), iconSlots(menu.getInventory()));
  }

  @Test
  public void threeRequirementsSitOnTheCentreColumn() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "B");
    plugin.getEconomy().setPlayer(player, 0);

    RankupMenu menu = openRankup(player);
    assertEquals(ConfirmScreen.State.UNMET, menu.getState());

    assertEquals(List.of(21, 22, 23), iconSlots(menu.getInventory()));
  }

  @Test
  public void thePanelTheButtonAndTheNavShareTheCentreColumn() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 0);

    RankupMenu menu = openRankup(player);

    assertNotNull(menu.getInventory().getItem(13), "the info panel stays at the top centre");
    assertEquals(31, menu.getActionSlot(), "the action button belongs under the requirement row");
    assertEquals(40, menu.getHomeSlot());
    assertEquals(4, menu.getActionSlot() % 9, "everything functional sits on column 4");
  }

  @Test
  public void meetingTheRequirementsKeepsTheSameGrid() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    player.setLevel(10);
    player.addAttachment(plugin, "rankup.symmetry.a", true);
    player.addAttachment(plugin, "rankup.symmetry.b", true);
    plugin.getEconomy().setPlayer(player, 500);

    RankupMenu menu = openRankup(player);
    assertEquals(ConfirmScreen.State.READY, menu.getState());

    // the ready screen is the same grid, not a different one: icons in place, the button green
    assertEquals(List.of(20, 21, 23, 24), iconSlots(menu.getInventory()));
    assertEquals(menu.getActionSlot(), menu.getConfirmSlot());
    assertEquals(Material.LIME_CONCRETE,
        menu.getInventory().getItem(menu.getActionSlot()).getType());
    assertFalse(contains(menu.getInventory(), Material.RED_CONCRETE),
        "the separate cancel button is gone; Back and Close in the nav bar replace it");
  }

  /** The requirement-row slots holding an icon rather than the border filler. */
  private static List<Integer> iconSlots(Inventory inventory) {
    List<Integer> slots = new ArrayList<>();
    for (int slot = ROW_FROM; slot <= ROW_TO; slot++) {
      ItemStack item = inventory.getItem(slot);
      if (item != null && item.getType() != Material.BLACK_STAINED_GLASS_PANE) {
        slots.add(slot);
      }
    }
    return slots;
  }

  private static boolean contains(Inventory inventory, Material material) {
    for (ItemStack item : inventory.getContents()) {
      if (item != null && item.getType() == material) {
        return true;
      }
    }
    return false;
  }
}
