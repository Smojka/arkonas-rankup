package com.arkonas.ranks.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.RankupTest;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /maxprestige} companion command. The prestigerequirements fixture prestiges a player at the
 * top rank B (to A, granting P1) for 10000 money.
 */
public class MaxPrestigeCommandTest extends RankupTest {

  public MaxPrestigeCommandTest() {
    super("prestigerequirements");
  }

  @Test
  public void maxPrestigeAdvancesAnEligiblePlayer() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.maxprestige", true);
    plugin.getEconomy().setPlayer(player, 10000);
    groupProvider.transferGroup(player.getUniqueId(), null, "B"); // at the top rank

    server.dispatchCommand(player, "maxprestige");

    assertTrue(groupProvider.inGroup(player.getUniqueId(), "P1"),
        "the player should have prestiged into P1");
    assertFalse(groupProvider.inGroup(player.getUniqueId(), "B"),
        "the top rank group should have been transferred away on prestige");
  }

  @Test
  public void maxPrestigeDoesNothingForANonTopPlayer() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.maxprestige", true);
    plugin.getEconomy().setPlayer(player, 10000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A"); // not the top rank

    server.dispatchCommand(player, "maxprestige");

    assertFalse(groupProvider.inGroup(player.getUniqueId(), "P1"));
    assertTrue(groupProvider.inGroup(player.getUniqueId(), "A"));
  }
}
