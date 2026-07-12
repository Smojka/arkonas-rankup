package com.arkonas.ranks.requirements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.arkonas.ranks.RankupTest;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * A plugin-hook requirement whose progress OR total source throws (dependency not loaded, API drift)
 * must fail closed — read as unmet — rather than propagate the exception out of /rankup, the menus or
 * a placeholder render.
 */
public class FailClosedRequirementTest extends RankupTest {

  private ProgressiveRequirement throwing(boolean totalThrows, boolean progressThrows) {
    ProgressiveRequirement req = new ProgressiveRequirement(plugin, "throwtest") {
      @Override
      public double getTotal(Player player) {
        if (totalThrows) {
          throw new RuntimeException("total hook down");
        }
        return getValueDouble();
      }

      @Override
      public double getProgress(Player player) {
        if (progressThrows) {
          throw new IllegalStateException("progress hook down");
        }
        return 0;
      }

      @Override
      public Requirement clone() {
        return this;
      }
    };
    req.setValue("10");
    return req;
  }

  @Test
  public void throwingProgressFailsClosed() {
    PlayerMock player = server.addPlayer();
    ProgressiveRequirement req = throwing(false, true);

    assertEquals(1.0, req.getRemaining(player), 0.0001, "a throwing hook should read as remaining");
    assertFalse(req.check(player), "and therefore as an unmet requirement");
  }

  @Test
  public void throwingTotalFailsClosed() {
    PlayerMock player = server.addPlayer();
    ProgressiveRequirement req = throwing(true, false);

    // getTotal is now inside the fail-closed guard too (PlaceholderRequirement.getTotal can throw)
    assertEquals(1.0, req.getRemaining(player), 0.0001);
    assertFalse(req.check(player));
  }
}
