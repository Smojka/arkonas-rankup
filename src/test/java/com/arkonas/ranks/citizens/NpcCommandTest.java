package com.arkonas.ranks.citizens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.citizens.NpcCommand.Dispatch;
import org.junit.jupiter.api.Test;

/** Pure NPC action-line parsing (console/player routing, %player%, slash stripping). */
class NpcCommandTest {

  @Test
  void playerCommandByDefault() {
    Dispatch dispatch = NpcCommand.parse("rankup", "Steve");
    assertFalse(dispatch.console());
    assertEquals("rankup", dispatch.command());
  }

  @Test
  void consolePrefixRoutesToConsoleCaseInsensitive() {
    Dispatch dispatch = NpcCommand.parse("[CONSOLE] give %player% diamond", "Steve");
    assertTrue(dispatch.console());
    assertEquals("give Steve diamond", dispatch.command());
  }

  @Test
  void explicitPlayerPrefixIsStripped() {
    Dispatch dispatch = NpcCommand.parse("[player] warp ranks", "Steve");
    assertFalse(dispatch.console());
    assertEquals("warp ranks", dispatch.command());
  }

  @Test
  void leadingSlashStripped() {
    assertEquals("maxrankup", NpcCommand.parse("/maxrankup", "Steve").command());
  }

  @Test
  void playerPlaceholderRendered() {
    assertEquals("pay Steve 100", NpcCommand.parse("pay %player% 100", "Steve").command());
  }

  @Test
  void nullLineIsBlank() {
    assertTrue(NpcCommand.parse(null, "Steve").command().isBlank());
  }
}
