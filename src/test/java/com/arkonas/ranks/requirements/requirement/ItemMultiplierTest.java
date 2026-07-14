package com.arkonas.ranks.requirements.requirement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.RankupTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** A cost sale also discounts item-count requirements (rounded), matching money/XP/token behaviour. */
class ItemMultiplierTest extends RankupTest {

  @Test
  void saleDiscountsItemCostAndDeduction() {
    ItemDeductibleRequirement requirement = new ItemDeductibleRequirement(plugin, "item");
    requirement.setValue("DIAMOND 20");
    PlayerMock player = server.addPlayer();
    player.getInventory().addItem(new ItemStack(Material.DIAMOND, 40));

    assertEquals(20.0, requirement.getTotal(player)); // no sale
    assertTrue(requirement.check(player));

    plugin.getMultipliers().setEventMultiplier(0.5, System.currentTimeMillis() + 3_600_000L);
    assertEquals(10.0, requirement.getTotal(player)); // 20 * 0.5

    requirement.apply(player); // deducts round(20 * 0.5) = 10 diamonds
    assertEquals(30.0, requirement.getProgress(player));
  }
}
