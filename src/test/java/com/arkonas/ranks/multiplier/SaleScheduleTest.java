package com.arkonas.ranks.multiplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.multiplier.SaleSchedule.Window;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure recurring-sale window logic: parsing, coverage, overlap, malformed skipping. */
class SaleScheduleTest {

  private static Map<String, Object> window(Object days, String start, String end, Object factor) {
    return Map.of("days", days, "start", start, "end", end, "factor", factor);
  }

  @Test
  void activeOnlyInsideWindow() {
    SaleSchedule schedule = new SaleSchedule(
        List.of(new Window(Set.of(DayOfWeek.SATURDAY), 1080, 1200, 0.5))); // 18:00-20:00
    assertEquals(0.5, schedule.activeFactor(DayOfWeek.SATURDAY, 1140)); // 19:00 inside
    assertEquals(1.0, schedule.activeFactor(DayOfWeek.SATURDAY, 1200)); // 20:00 exclusive end
    assertEquals(1.0, schedule.activeFactor(DayOfWeek.SATURDAY, 600));  // 10:00 outside
    assertEquals(1.0, schedule.activeFactor(DayOfWeek.SUNDAY, 1140));   // wrong day
  }

  @Test
  void overlappingWindowsPickLowestFactor() {
    SaleSchedule schedule = new SaleSchedule(List.of(
        new Window(Set.of(DayOfWeek.FRIDAY), 0, 1440, 0.9),
        new Window(Set.of(DayOfWeek.FRIDAY), 1080, 1200, 0.5)));
    assertEquals(0.5, schedule.activeFactor(DayOfWeek.FRIDAY, 1140)); // both cover -> best discount
    assertEquals(0.9, schedule.activeFactor(DayOfWeek.FRIDAY, 600));  // only the all-day one
  }

  @Test
  void parsesConfigMapsIncludingAllDays() {
    SaleSchedule schedule = SaleSchedule.fromMaps(List.of(
        window(List.of("SATURDAY", "SUNDAY"), "18:00", "20:00", 0.5),
        window(List.of("ALL"), "00:00", "06:00", "0.75")));
    assertEquals(0.5, schedule.activeFactor(DayOfWeek.SUNDAY, 1140));
    assertEquals(0.75, schedule.activeFactor(DayOfWeek.WEDNESDAY, 120)); // ALL covers Wednesday 02:00
    assertEquals(1.0, schedule.activeFactor(DayOfWeek.WEDNESDAY, 1140));
  }

  @Test
  void malformedWindowsSkipped() {
    SaleSchedule schedule = SaleSchedule.fromMaps(List.of(
        window(List.of("MONDAY"), "20:00", "18:00", 0.5),   // start >= end
        window(List.of("MONDAY"), "25:00", "26:00", 0.5),   // bad time
        window(List.of("MONDAY"), "10:00", "11:00", "abc"), // bad factor
        window(List.of("NOPE"), "10:00", "11:00", 0.5),     // unknown day -> empty set
        window(List.of("MONDAY"), "12:00", "13:00", 0.5))); // the one valid window
    assertEquals(0.5, schedule.activeFactor(DayOfWeek.MONDAY, 750)); // 12:30
    assertEquals(1.0, schedule.activeFactor(DayOfWeek.MONDAY, 630)); // 10:30 (all malformed)
  }

  @Test
  void parseTimeBounds() {
    assertEquals(0, SaleSchedule.parseTime("00:00"));
    assertEquals(1140, SaleSchedule.parseTime("19:00"));
    assertEquals(-1, SaleSchedule.parseTime("24:00"));
    assertEquals(-1, SaleSchedule.parseTime("12:60"));
    assertEquals(-1, SaleSchedule.parseTime("noon"));
  }

  @Test
  void emptyScheduleWhenNoConfig() {
    assertTrue(SaleSchedule.fromConfig(null).isEmpty());
    assertTrue(new SaleSchedule(List.of()).isEmpty());
  }
}
