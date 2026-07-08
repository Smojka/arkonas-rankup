package com.arkonas.ranks.multiplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.multiplier.BoosterCommands.Result;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

/** Pure /aru booster logic: duration/factor parsing, apply/clear/status, messages. */
class BoosterCommandsTest {

  private static MultiplierService service() {
    return new MultiplierService(1.0, new LinkedHashMap<>());
  }

  private static final long NOW = 1_000_000_000_000L;

  // --- parseDurationSeconds -------------------------------------------------------------------

  @Test
  void durationUnits() {
    assertEquals(600, BoosterCommands.parseDurationSeconds("600"));   // bare = seconds
    assertEquals(600, BoosterCommands.parseDurationSeconds("600s"));
    assertEquals(1800, BoosterCommands.parseDurationSeconds("30m"));
    assertEquals(7200, BoosterCommands.parseDurationSeconds("2h"));
    assertEquals(86400, BoosterCommands.parseDurationSeconds("1d"));
    assertEquals(1800, BoosterCommands.parseDurationSeconds("30M")); // case-insensitive
  }

  @Test
  void durationInvalid() {
    assertEquals(-1, BoosterCommands.parseDurationSeconds("30x"));  // unknown unit
    assertEquals(-1, BoosterCommands.parseDurationSeconds("abc"));
    assertEquals(-1, BoosterCommands.parseDurationSeconds("0"));    // must be positive
    assertEquals(-1, BoosterCommands.parseDurationSeconds("-5m"));
    assertEquals(-1, BoosterCommands.parseDurationSeconds(""));
  }

  // --- parseFactor ----------------------------------------------------------------------------

  @Test
  void factorParsing() {
    assertEquals(0.5, BoosterCommands.parseFactor("0.5"));
    assertEquals(2.0, BoosterCommands.parseFactor("2"));
    assertNull(BoosterCommands.parseFactor("0"));
    assertNull(BoosterCommands.parseFactor("-1"));
    assertNull(BoosterCommands.parseFactor("abc"));
  }

  // --- formatDuration -------------------------------------------------------------------------

  @Test
  void formatDurationReadable() {
    assertEquals("30m", BoosterCommands.formatDuration(1800));
    assertEquals("1h 30m", BoosterCommands.formatDuration(5400));
    assertEquals("1d 2h", BoosterCommands.formatDuration(93600));
    assertEquals("45s", BoosterCommands.formatDuration(45));
  }

  // --- handle ---------------------------------------------------------------------------------

  @Test
  void setActivatesBooster() {
    MultiplierService service = service();
    Result result = BoosterCommands.handle(service, new String[]{"0.5", "30m"}, NOW);
    assertTrue(result.success());
    assertTrue(service.isEventActive(NOW));
    assertEquals(0.5, service.eventFactor());
    assertEquals(NOW + 1800_000L, service.eventUntilMillis());
    assertTrue(result.message().contains("x0.5"));
    assertTrue(result.message().contains("30m"));
  }

  @Test
  void clearEndsBooster() {
    MultiplierService service = service();
    BoosterCommands.handle(service, new String[]{"0.5", "1h"}, NOW);
    Result result = BoosterCommands.handle(service, new String[]{"clear"}, NOW);
    assertTrue(result.success());
    assertFalse(service.isEventActive(NOW));
  }

  @Test
  void statusReportsActiveAndInactive() {
    MultiplierService service = service();
    assertTrue(BoosterCommands.handle(service, new String[0], NOW).message().contains("No booster"));

    BoosterCommands.handle(service, new String[]{"0.75", "10m"}, NOW);
    Result status = BoosterCommands.handle(service, new String[]{"status"}, NOW);
    assertTrue(status.message().contains("x0.75"));
    assertTrue(status.message().contains("remaining"));
  }

  @Test
  void invalidInputsFailWithoutMutating() {
    MultiplierService service = service();
    assertFalse(BoosterCommands.handle(service, new String[]{"abc", "30m"}, NOW).success());
    assertFalse(BoosterCommands.handle(service, new String[]{"0.5", "30x"}, NOW).success());
    assertFalse(BoosterCommands.handle(service, new String[]{"0.5"}, NOW).success()); // missing duration
    assertFalse(service.isEventActive(NOW));
  }
}
