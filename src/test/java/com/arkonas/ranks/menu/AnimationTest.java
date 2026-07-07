package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;

/**
 * The border chase must move (at least one ring slot changes each few frames)
 * while never disturbing the functional slots — the dirty-slot guarantee.
 */
public class AnimationTest extends RankupTest {

  public AnimationTest() {
    super("menus");
  }

  @Test
  public void borderChaseMovesButContentStaysStable() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getMenuModule().openHub(player);
    Inventory inv = player.getOpenInventory().getTopInventory();
    int headSlot = 13;
    Material headBefore = type(inv.getItem(headSlot));
    Material[] before = types(inv);

    server.getScheduler().performTicks(4);

    Material[] after = types(inv);
    int changed = 0;
    for (int i = 0; i < before.length; i++) {
      if (before[i] != after[i]) {
        changed++;
      }
    }
    assertTrue(changed >= 1, "the border chase should have lit at least one ring slot");

    // the functional player-head slot must be untouched by the animation
    assertEquals(headBefore, type(inv.getItem(headSlot)), "content slots must not change");
    assertEquals(Material.PLAYER_HEAD, type(inv.getItem(headSlot)));
  }

  private static Material[] types(Inventory inv) {
    Material[] out = new Material[inv.getSize()];
    for (int i = 0; i < out.length; i++) {
      out[i] = type(inv.getItem(i));
    }
    return out;
  }

  private static Material type(ItemStack item) {
    return item == null ? Material.AIR : item.getType();
  }
}
