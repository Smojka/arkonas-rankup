package com.arkonas.ranks.multiplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.multiplier.SaleSchedule.Window;
import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure scheduler precedence: manual /aru booster always wins over recurring sales. */
class SaleSchedulerTest {

  private static final long NOW = 1_000_000_000_000L;
  private static final long REFRESH = 70_000L;

  private static MultiplierService service() {
    return new MultiplierService(1.0, new LinkedHashMap<>());
  }

  private static SaleSchedule saturday() {
    return new SaleSchedule(List.of(new Window(Set.of(DayOfWeek.SATURDAY), 1080, 1200, 0.5)));
  }

  @Test
  void activeWindowSetsScheduledEvent() {
    MultiplierService service = service();
    SaleScheduler.apply(saturday(), service, DayOfWeek.SATURDAY, 1140, NOW, REFRESH);
    assertTrue(service.isEventActive(NOW));
    assertEquals(0.5, service.eventFactor());
    assertFalse(service.isManualEvent());
  }

  @Test
  void noWindowClearsScheduledSlot() {
    MultiplierService service = service();
    SaleScheduler.apply(saturday(), service, DayOfWeek.MONDAY, 1140, NOW, REFRESH);
    assertFalse(service.isEventActive(NOW));
  }

  @Test
  void manualBoosterIsNotClobbered() {
    MultiplierService service = service();
    service.setEventMultiplier(0.25, NOW + 3_600_000L); // manual booster, 1h
    // even outside any window, and even inside one, the manual booster stays
    SaleScheduler.apply(saturday(), service, DayOfWeek.SATURDAY, 1140, NOW, REFRESH);
    assertTrue(service.isManualEvent());
    assertEquals(0.25, service.eventFactor());
  }

  @Test
  void schedulerResumesAfterManualBoosterExpires() {
    MultiplierService service = service();
    service.setEventMultiplier(0.25, NOW - 1000L); // manual booster already expired
    SaleScheduler.apply(saturday(), service, DayOfWeek.SATURDAY, 1140, NOW, REFRESH);
    assertFalse(service.isManualEvent()); // scheduled took over
    assertEquals(0.5, service.eventFactor());
    assertTrue(service.isEventActive(NOW));
  }
}
