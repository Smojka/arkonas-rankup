package com.arkonas.ranks.ranks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.requirements.Requirement;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Regression test for the per-rank {@code cost-multiplier}: it was parsed nowhere on the runtime
 * deserialization path, so a configured discount silently did nothing. Rank A here has
 * {@code cost-multiplier: 0.5} on a {@code money 100} requirement, so the real cost is 50.
 */
public class PerRankCostMultiplierTest extends RankupTest {

  public PerRankCostMultiplierTest() {
    super("costmultiplier");
  }

  private PlayerMock player(int money) {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, money);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    player.addAttachment(plugin, "rankup.rankup", true);
    return player;
  }

  private Requirement moneyRequirement(PlayerMock player) {
    return plugin.getRankups().getByPlayer(player).getRank()
        .getRequirements().getRequirement(player, "money");
  }

  @Test
  public void costMultiplierHalvesTheEffectiveMoneyCost() {
    PlayerMock player = player(1000);
    assertEquals(50.0, moneyRequirement(player).getTotal(player), 0.001,
        "cost-multiplier 0.5 should halve the 100 money requirement to 50");
  }

  @Test
  public void playerWithTheDiscountedAmountCanRankUp() {
    PlayerMock player = player(50); // exactly the discounted cost (100 * 0.5)

    server.dispatchCommand(player, "rankup");

    assertTrue(groupProvider.inGroup(player.getUniqueId(), "B"),
        "50 money should be enough once the 0.5 multiplier is applied");
    assertEquals(0, plugin.getEconomy().getBalance(player), 0.001,
        "the discounted cost of 50 should have been deducted");
  }

  @Test
  public void belowTheDiscountedAmountCannotRankUp() {
    PlayerMock player = player(49);

    server.dispatchCommand(player, "rankup");

    assertFalse(groupProvider.inGroup(player.getUniqueId(), "B"),
        "49 money is below the discounted cost of 50");
  }
}
