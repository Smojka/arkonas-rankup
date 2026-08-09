package com.arkonas.ranks.requirements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.ranks.requirements.ListRankRequirements;
import com.arkonas.ranks.requirements.requirement.MoneyDeductibleRequirement;

/**
 * A rank must never be granted unless its cost was actually taken. The check and the charge are
 * separate calls, so these cover the two ways that gap can be abused: charging a player who no
 * longer meets the requirements, and granting the rank when the economy refused the withdrawal.
 */
public class DeductionSafetyTest extends RankupTest {

  private MoneyDeductibleRequirement money(int amount) {
    MoneyDeductibleRequirement requirement = new MoneyDeductibleRequirement(plugin, "money");
    requirement.setValue(String.valueOf(amount));
    return requirement;
  }

  @Test
  public void deductionRefusesWhenTheRequirementIsNoLongerMet() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 10);
    ListRankRequirements requirements = new ListRankRequirements(List.of(money(1000)));

    assertFalse(requirements.hasRequirements(player));
    assertThrows(IllegalStateException.class, () -> requirements.applyRequirements(player),
        "deducting without meeting the requirements would be a free rankup");
    assertEquals(10.0, plugin.getEconomy().getBalance(player), "nothing may be taken");
  }

  @Test
  public void aRefusedWithdrawalThrowsRatherThanReportingSuccess() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 10);

    // the economy refuses (balance below the cost); the deduction must not return quietly, because
    // its caller grants the rank as soon as it does
    assertThrows(IllegalStateException.class, () -> money(1000).apply(player, 1.0));
    assertEquals(10.0, plugin.getEconomy().getBalance(player));
  }

  @Test
  public void applyCostReportsFailureAndLeavesTheRankAlone() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 10);

    RankElement<Rank> element = plugin.getRankups().getByPlayer(player);
    assertFalse(plugin.getHelper().applyCost(player, element.getRank()),
        "an unaffordable cost must be reported as a failure, not swallowed");

    plugin.getHelper().rankup(player);
    assertTrue(groupProvider.inGroup(player.getUniqueId(), "A"), "the player must not advance");
    assertFalse(groupProvider.inGroup(player.getUniqueId(), "B"));
  }
}
