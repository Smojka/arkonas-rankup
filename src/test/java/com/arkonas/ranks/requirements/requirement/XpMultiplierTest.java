package com.arkonas.ranks.requirements.requirement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.RankupTest;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * End-to-end: a cost sale/booster discounts an XP-level requirement's shown cost, affordability and
 * actual deduction — proving the multiplier now applies to non-money currencies, not just money.
 */
class XpMultiplierTest extends RankupTest {

  @Test
  void saleDiscountsXpCostAndDeduction() {
    XpLevelDeductibleRequirement requirement = new XpLevelDeductibleRequirement(plugin, "xp-level");
    requirement.setValue("10");
    PlayerMock player = server.addPlayer();
    player.setLevel(20);

    // no active sale: full cost
    assertEquals(10.0, requirement.getTotal(player));
    assertTrue(requirement.check(player));

    // 50% off: both the displayed total and the deduction halve
    plugin.getMultipliers().setEventMultiplier(0.5, System.currentTimeMillis() + 3_600_000L);
    assertEquals(5.0, requirement.getTotal(player));

    requirement.apply(player); // deducts round(10 * 0.5) = 5 levels
    assertEquals(15, player.getLevel());
  }

  @Test
  void perRankFactorStacksWithSale() {
    XpLevelDeductibleRequirement requirement = new XpLevelDeductibleRequirement(plugin, "xp-level");
    requirement.setValue("10");
    requirement.setPerRankFactor(0.5); // this rank costs half
    PlayerMock player = server.addPlayer();
    player.setLevel(40);

    assertEquals(5.0, requirement.getTotal(player)); // 10 * 0.5 per-rank

    // a 50% sale stacks on top of the per-rank factor
    plugin.getMultipliers().setEventMultiplier(0.5, System.currentTimeMillis() + 3_600_000L);
    assertEquals(2.5, requirement.getTotal(player)); // 10 * 0.5 * 0.5
  }

  @Test
  void neutralMultiplierLeavesCostUnchanged() {
    XpLevelDeductibleRequirement requirement = new XpLevelDeductibleRequirement(plugin, "xp-level");
    requirement.setValue("8");
    PlayerMock player = server.addPlayer();
    player.setLevel(20);

    assertEquals(8.0, requirement.getTotal(player)); // default factor 1.0
    requirement.apply(player);
    assertEquals(12, player.getLevel());
  }
}
