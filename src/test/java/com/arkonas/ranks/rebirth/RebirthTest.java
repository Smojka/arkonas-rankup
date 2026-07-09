package com.arkonas.ranks.rebirth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.RankupTest;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Rebirth tier over the {@code rebirth} fixture: rank ladder A -> B (top group B), rebirth groups
 * reborn1 -> reborn2, requires top-rank, resets to the first rank, costs money 100.
 */
public class RebirthTest extends RankupTest {

  public RebirthTest() {
    super("rebirth");
  }

  private boolean in(PlayerMock player, String group) {
    return groupProvider.inGroup(player.getUniqueId(), group);
  }

  /** A player sitting at the top rank (group B) with enough money. */
  private PlayerMock atTop(int money) {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, money);
    groupProvider.transferGroup(player.getUniqueId(), null, "B");
    return player;
  }

  @Test
  public void rebirthAdvancesAndResets() {
    PlayerMock player = atTop(100);

    assertTrue(plugin.getRebirth().rebirth(player));

    assertTrue(in(player, "reborn1"), "should gain the first rebirth group");
    assertTrue(in(player, "A"), "should be reset to the first rank");
    assertFalse(in(player, "B"), "should no longer be at the top rank");
    assertEquals(0, plugin.getEconomy().getBalance(player), 0);
  }

  @Test
  public void cannotRebirthBelowTop() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 100);
    groupProvider.transferGroup(player.getUniqueId(), null, "A"); // not at top

    assertFalse(plugin.getRebirth().rebirth(player));
    assertFalse(in(player, "reborn1"));
    assertEquals(100, plugin.getEconomy().getBalance(player), 0);
  }

  @Test
  public void requirementsEnforced() {
    PlayerMock player = atTop(50); // needs 100

    assertFalse(plugin.getRebirth().rebirth(player));
    assertFalse(in(player, "reborn1"));
    assertTrue(in(player, "B"), "should not be reset when the rebirth fails");
    assertEquals(50, plugin.getEconomy().getBalance(player), 0);
  }

  @Test
  public void secondRebirthAdvancesToNextGroup() {
    PlayerMock player = atTop(100);
    groupProvider.transferGroup(player.getUniqueId(), null, "reborn1");

    assertTrue(plugin.getRebirth().rebirth(player));

    assertTrue(in(player, "reborn2"));
    assertFalse(in(player, "reborn1"));
    assertTrue(in(player, "A"));
  }

  @Test
  public void maxedRebirthBlocked() {
    PlayerMock player = atTop(100);
    groupProvider.transferGroup(player.getUniqueId(), null, "reborn2"); // last group

    assertFalse(plugin.getRebirth().rebirth(player));
    assertTrue(in(player, "reborn2"));
    assertTrue(in(player, "B"), "no reset on a blocked rebirth");
  }

  @Test
  public void commandPerformsRebirth() {
    PlayerMock player = atTop(100);
    player.addAttachment(plugin, "rankup.rebirth", true);

    server.dispatchCommand(player, "rebirth");

    assertTrue(in(player, "reborn1"));
  }

  private String placeholder(PlayerMock player, String params) {
    return plugin.getPlaceholders().getExpansion().placeholder(player, params);
  }

  @Test
  public void rebirthPlaceholders() {
    PlayerMock player = atTop(100);
    assertEquals("0", placeholder(player, "rebirth_count"));
    assertEquals("None", placeholder(player, "current_rebirth"));
    assertEquals("reborn1", placeholder(player, "next_rebirth"));

    assertTrue(plugin.getRebirth().rebirth(player));

    assertEquals("1", placeholder(player, "rebirth_count"));
    assertEquals("reborn1", placeholder(player, "current_rebirth"));
    assertEquals("reborn2", placeholder(player, "next_rebirth"));
  }
}
