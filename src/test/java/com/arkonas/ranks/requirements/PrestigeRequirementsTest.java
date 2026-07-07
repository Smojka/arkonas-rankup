package com.arkonas.ranks.requirements;

import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.Test;
import com.arkonas.ranks.RankupTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrestigeRequirementsTest extends RankupTest {

  public PrestigeRequirementsTest() {
    super("prestigerequirements");
  }

  @Test
  public void testPrestigeRequirements() {
    PlayerMock player = server.addPlayer();

    plugin.getPermissions().transferGroup(player.getUniqueId(), null, "p1");
    plugin.getPermissions().transferGroup(player.getUniqueId(), null, "a");

    plugin.getEconomy().setPlayer(player, 200);

    plugin.getHelper().rankup(player);

    assertTrue(plugin.getPermissions().inGroup(player.getUniqueId(), "b"), "player is not in group b");
    assertEquals(0, plugin.getEconomy().getBalance(player), "prestige requirements did not take correct amount of money");
  }
}
