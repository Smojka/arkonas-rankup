package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.RankupTest;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Open-reveal transition (fixture {@code menusreveal}: open-reveal on, speed 1, border-chase off).
 * At open every slot is hidden behind the base pane; the ticker wipes the real grid in over frames.
 */
public class OpenRevealTest extends RankupTest {

  public OpenRevealTest() {
    super("menusreveal");
  }

  private long nonBaseSlots(Inventory inv, Material base) {
    long count = 0;
    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack item = inv.getItem(i);
      if (item != null && item.getType() != base) {
        count++;
      }
    }
    return count;
  }

  @Test
  public void gridHiddenAtOpenThenRevealed() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.ranks", true);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    server.dispatchCommand(player, "ranks");
    Inventory top = player.getOpenInventory().getTopInventory();

    // at open, every slot shows the base pane (whole grid hidden)
    ItemStack first = top.getItem(0);
    assertNotNull(first, "slot 0 should hold the base pane at open");
    Material base = first.getType();
    assertEquals(0, nonBaseSlots(top, base), "every slot hidden behind the base pane at open");

    // after enough frames the wipe finishes and real content is visible
    server.getScheduler().performTicks(200);
    assertTrue(nonBaseSlots(top, base) > 0, "content should be revealed after the wipe");
  }

  @Test
  public void clickDuringRevealSnapsGridVisible() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.ranks", true);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    server.dispatchCommand(player, "ranks");
    Inventory top = player.getOpenInventory().getTopInventory();
    Material base = top.getItem(0).getType();
    assertEquals(0, nonBaseSlots(top, base), "hidden right after open");

    // a click mid-reveal must snap the grid fully visible (and not fire a hidden action)
    player.simulateInventoryClick(4);

    assertTrue(nonBaseSlots(top, base) > 0, "click during the reveal reveals the whole grid");
  }
}
