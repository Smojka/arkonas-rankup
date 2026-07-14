package com.arkonas.ranks.multiplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.RankupTest;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the deferred-init bug on the recurring-sale scheduler: it is started inside
 * the deferred {@code refreshRanks()} tick, so the original onEnable start (gated on a still-null
 * {@code multipliers} field) never ran. This boots the plugin with an always-active sale window and
 * asserts the scheduler reflects it into the multiplier service.
 */
public class SaleSchedulerWiringTest extends RankupTest {

  public SaleSchedulerWiringTest() {
    super("salescheduler");
  }

  @Test
  public void scheduledSaleReflectsIntoMultiplierService() {
    server.getScheduler().performTicks(2); // let the started scheduler task run

    assertTrue(plugin.getMultipliers().isEventActive(System.currentTimeMillis()),
        "the recurring-sale scheduler should reflect the active window into the multiplier service");
    assertEquals(0.5, plugin.getMultipliers().eventFactor(), 0.001,
        "the active event factor should match the configured sale");
  }

  @Test
  public void reloadRestartsTheScheduler() {
    plugin.reload(false);
    server.getScheduler().performTicks(2);

    assertTrue(plugin.getMultipliers().isEventActive(System.currentTimeMillis()),
        "the scheduler should be restarted after /aru reload");
  }
}
