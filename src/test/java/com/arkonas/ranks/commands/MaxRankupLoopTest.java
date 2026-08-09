package com.arkonas.ranks.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.hook.GroupProvider;

/**
 * {@code /maxrankup} loops until the player can no longer afford the next rank. If the group change
 * never takes — a permission backend that silently fails, or {@code permission-rankup} mode where
 * {@code transferGroup} is a no-op — the player stays on the same rank and every pass charges them
 * for it again, draining their balance and spinning the main thread until it runs out.
 */
public class MaxRankupLoopTest extends RankupTest {

  /** A permission backend that accepts the initial seed and then silently drops every change. */
  private static final class StuckGroupProvider implements GroupProvider {
    private final Set<String> groups = new HashSet<>();
    private boolean stuck;

    @Override
    public boolean inGroup(UUID uuid, String group) {
      return groups.contains(uuid + "|" + group.toLowerCase());
    }

    @Override
    public void transferGroup(UUID uuid, String oldGroup, String group) {
      if (stuck) {
        return; // the change is silently dropped, exactly as a failing backend would
      }
      if (oldGroup != null) {
        groups.remove(uuid + "|" + oldGroup.toLowerCase());
      }
      groups.add(uuid + "|" + group.toLowerCase());
    }
  }

  private final StuckGroupProvider stuckProvider = new StuckGroupProvider();

  @Override
  protected GroupProvider createGroupProvider() {
    return stuckProvider;
  }

  @Test
  public void aStalledRankChangeChargesAtMostOnce() {
    PlayerMock player = server.addPlayer();
    stuckProvider.transferGroup(player.getUniqueId(), null, "A");
    // far more than the A -> B cost of 1000, so an unguarded loop would keep buying the same rank
    plugin.getEconomy().setPlayer(player, 100_000);
    stuckProvider.stuck = true;

    new MaxRankupCommand(plugin).onCommand(player, null, "maxrankup", new String[0]);

    assertTrue(stuckProvider.inGroup(player.getUniqueId(), "A"), "the rank could not change");
    assertEquals(99_000.0, plugin.getEconomy().getBalance(player),
        "the player must be charged for one rankup at most, not once per loop pass");
  }

  @Test
  public void rankupOnceReportsNoProgressSoAutoMaxStops() {
    PlayerMock player = server.addPlayer();
    stuckProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 100_000);
    stuckProvider.stuck = true;

    // auto-max loops `while (rankupOnce(...))`, so the stall has to surface as a false return
    assertFalse(plugin.getHelper().rankupOnce(player),
        "a rankup that left the player on the same rank has not advanced them");
    assertEquals(99_000.0, plugin.getEconomy().getBalance(player),
        "and the next pass must not charge them for the same rank again");
  }
}
