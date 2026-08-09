package com.arkonas.ranks.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

/**
 * Reward commands are rendered with PlaceholderAPI applied, and a placeholder can resolve to text a
 * player controls (a nickname, a display name). These cover the sanitising that stops such text from
 * changing how the console reads the command.
 */
class ConsoleCommandTest {

  private static final Logger LOGGER = Logger.getLogger("ConsoleCommandTest");

  @Test
  void stripsControlCharactersThatWouldSplitTheCommand() {
    assertEquals("say hi op Bob",
        ConsoleCommand.sanitise(LOGGER, "say hi\nop Bob"));
    assertEquals("say hi",
        ConsoleCommand.sanitise(LOGGER, "say hi\r"));
    // replaced with a space, never dropped, so "a" and "b" cannot fuse into one token
    assertEquals("say a b",
        ConsoleCommand.sanitise(LOGGER, "say a" + (char) 0 + "b"));
  }

  @Test
  void stripsLeadingSlashes() {
    assertEquals("say hi", ConsoleCommand.sanitise(LOGGER, "/say hi"));
    assertEquals("say hi", ConsoleCommand.sanitise(LOGGER, "  //say hi  "));
  }

  @Test
  void rejectsBlankAndNull() {
    assertNull(ConsoleCommand.sanitise(LOGGER, null));
    assertNull(ConsoleCommand.sanitise(LOGGER, "   "));
    assertNull(ConsoleCommand.sanitise(LOGGER, "/"));
  }

  @Test
  void rejectsARunawayCommand() {
    assertNull(ConsoleCommand.sanitise(LOGGER, "say " + "x".repeat(4000)));
  }

  @Test
  void leavesAnOrdinaryCommandAlone() {
    assertEquals("eco give Bob 100", ConsoleCommand.sanitise(LOGGER, "eco give Bob 100"));
  }
}
