package com.arkonas.ranks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Auto-chain behaviour of {@link AutoRankup} over the bundled A -> B -> C -> D ladder
 * (money 1000 / 2500 / 5000+xp2). Config is mutated at runtime because AutoRankup reads the
 * {@code auto} section on every pass.
 */
public class AutoRankupTest extends RankupTest {

  private PlayerMock eligiblePlayer(int money, int level) {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, money);
    player.setLevel(level);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    player.addAttachment(plugin, "rankup.auto", true);
    return player;
  }

  @Test
  public void maxRankupClimbsToTopInOnePass() {
    PlayerMock player = eligiblePlayer(1000 + 2500 + 5000, 2);
    plugin.getConfig().set("auto.max", true);

    plugin.autoRankup.run();

    assertTrue(groupProvider.inGroup(player.getUniqueId(), "D"),
        "max rankup should climb A -> D in a single pass");
    assertEquals(0, plugin.getEconomy().getBalance(player), 0);
  }

  @Test
  public void withoutMaxOnlyAdvancesOneStep() {
    PlayerMock player = eligiblePlayer(1000 + 2500 + 5000, 2);
    // auto.max defaults to false

    plugin.autoRankup.run();

    assertTrue(groupProvider.inGroup(player.getUniqueId(), "B"));
    assertFalse(groupProvider.inGroup(player.getUniqueId(), "C"));
    assertEquals(7500, plugin.getEconomy().getBalance(player), 0);
  }

  @Test
  public void rankupDisabledSkipsPlayer() {
    PlayerMock player = eligiblePlayer(1000 + 2500 + 5000, 2);
    plugin.getConfig().set("auto.rankup", false);

    plugin.autoRankup.run();

    assertTrue(groupProvider.inGroup(player.getUniqueId(), "A"));
  }

  @Test
  public void requiresAutoPermission() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 5000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getConfig().set("auto.max", true);

    plugin.autoRankup.run();

    assertTrue(groupProvider.inGroup(player.getUniqueId(), "A"),
        "player without rankup.auto must not be advanced");
  }
}
