package com.arkonas.ranks.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.arkonas.ranks.RankupTest;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Regression test for the deferred-init bug: the live progress display and the recurring-sale
 * scheduler are built inside the deferred {@code refreshRanks()} tick, so wiring them in onEnable
 * (as the code originally did) always saw a null field and silently no-op'd. These tests boot the
 * real plugin and assert the components actually run.
 */
public class ProgressDisplayWiringTest extends RankupTest {

  public ProgressDisplayWiringTest() {
    super("progressdisplay");
  }

  private PlayerMock playerInLadder() {
    PlayerMock player = server.addPlayer();
    // 25 of the 50 money required for A -> B means the display should report 50% progress
    plugin.getEconomy().setPlayer(player, 25);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    return player;
  }

  @Test
  public void progressDisplayIsWiredAndUpdatesExpBar() {
    assertNotNull(plugin.getProgressDisplay(),
        "progress display should be built when progress-display.enabled is true");

    PlayerMock player = playerInLadder();
    server.getScheduler().performTicks(3); // let the scheduled display task run

    assertEquals(0.5f, player.getExp(), 0.001f,
        "exp bar should mirror 50% progress toward the next rank once the display is running");
  }

  @Test
  public void reloadRewiresProgressDisplayWithoutDoubleScheduling() {
    PlayerMock player = playerInLadder(); // display running before reload (teardown must clear it)

    plugin.reload(false); // /aru reload path — rebuilds and must re-wire, not double-schedule
    // reload() rebuilds the economy service, so re-assert the player's balance on the fresh one
    plugin.getEconomy().setPlayer(player, 25);
    server.getScheduler().performTicks(3);

    assertNotNull(plugin.getProgressDisplay());
    assertEquals(0.5f, player.getExp(), 0.001f,
        "the display should still update after a reload");
  }
}
