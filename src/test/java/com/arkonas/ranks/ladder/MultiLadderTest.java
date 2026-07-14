package com.arkonas.ranks.ladder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.ranks.Rankups;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Multi-ladder progression over the {@code multiladder} fixture: a default ladder A -> B -> C and a
 * second {@code mining} ladder mine1 -> mine2 -> mine3, each rank a distinct permission group.
 */
public class MultiLadderTest extends RankupTest {

  public MultiLadderTest() {
    super("multiladder");
  }

  private PlayerMock player() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 1000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    groupProvider.transferGroup(player.getUniqueId(), null, "mine1");
    return player;
  }

  private boolean in(PlayerMock player, String group) {
    return groupProvider.inGroup(player.getUniqueId(), group);
  }

  @Test
  public void bothLaddersLoad() {
    LadderRegistry ladders = plugin.getLadders();
    assertEquals(2, ladders.size());
    assertTrue(ladders.has("default"));
    assertTrue(ladders.has("mining"));
    assertTrue(ladders.hasMultiple());
  }

  @Test
  public void laddersProgressIndependently() {
    PlayerMock player = player();
    Rankups mining = plugin.getLadders().get("mining");

    plugin.getHelper().rankup(player, mining);
    assertTrue(in(player, "mine2"), "mining should advance");
    assertTrue(in(player, "A"), "default ladder must be untouched");
    assertFalse(in(player, "B"));
    assertEquals(950, plugin.getEconomy().getBalance(player), 0);

    plugin.getHelper().rankup(player, plugin.getLadders().getDefault());
    assertTrue(in(player, "B"), "default should advance");
    assertTrue(in(player, "mine2"), "mining must stay put");
    assertEquals(900, plugin.getEconomy().getBalance(player), 0);
  }

  @Test
  public void rankupCommandRoutesToNamedLadder() {
    PlayerMock player = player();
    player.addAttachment(plugin, "rankup.rankup", true);

    server.dispatchCommand(player, "rankup mining");

    assertTrue(in(player, "mine2"), "/rankup mining should advance the mining ladder");
    assertTrue(in(player, "A"), "default ladder must be untouched");
  }

  @Test
  public void nonMaxAutoAdvancesEveryLadderDespiteCooldown() {
    PlayerMock player = player();
    player.addAttachment(plugin, "rankup.auto", true);
    // a manual cooldown set: it must not let ladder 1's cooldown block the other ladders in one pass
    plugin.getConfig().set("cooldown", 5);
    // auto.max defaults to false, so this exercises the single-step-per-ladder branch

    new com.arkonas.ranks.AutoRankup(plugin).run();

    assertTrue(in(player, "B"), "default ladder should advance one step");
    assertTrue(in(player, "mine2"), "mining ladder should advance one step in the same pass");
  }

  @Test
  public void autoAdvancesEveryLadder() {
    PlayerMock player = player();
    player.addAttachment(plugin, "rankup.auto", true);
    plugin.getConfig().set("auto.max", true);

    new com.arkonas.ranks.AutoRankup(plugin).run();

    assertTrue(in(player, "C"), "default ladder should reach the top");
    assertTrue(in(player, "mine3"), "mining ladder should reach the top");
    assertEquals(800, plugin.getEconomy().getBalance(player), 0);
  }
}
