package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;

/**
 * With animation.enabled: false in menus.yml the ticker never starts, so nothing
 * moves between frames.
 */
public class AnimationDisabledTest extends RankupTest {

  public AnimationDisabledTest() {
    super("menusnoanim");
  }

  @Test
  public void noAnimationLeavesEveryturnUnchanged() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getMenuModule().openHub(player);
    Inventory inv = player.getOpenInventory().getTopInventory();
    Material[] before = types(inv);

    server.getScheduler().performTicks(8);

    assertArrayEquals(before, types(inv), "no slot should change while animation is disabled");
    assertTrue(plugin.getMenuModule().getOpenMenus().contains(
        (com.arkonas.ranks.menu.AbstractMenu) inv.getHolder()),
        "the menu is still open, just not animating");
  }

  private static Material[] types(Inventory inv) {
    Material[] out = new Material[inv.getSize()];
    for (int i = 0; i < out.length; i++) {
      ItemStack item = inv.getItem(i);
      out[i] = item == null ? Material.AIR : item.getType();
    }
    return out;
  }
}
