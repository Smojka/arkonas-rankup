package com.arkonas.ranks.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.data.LeaderboardEntry;
import com.arkonas.ranks.placeholders.LeaderboardPlaceholder.Request;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure leaderboard-placeholder parsing + rendering (the substrate for hologram/TAB plugins). */
class LeaderboardPlaceholderTest {

  private static LeaderboardEntry entry(String name, int count) {
    return new LeaderboardEntry(UUID.randomUUID(), name, count);
  }

  // --- parse ----------------------------------------------------------------------------------

  @Test
  void parsesRankupName() {
    Request request = LeaderboardPlaceholder.parse("top_1_name");
    assertFalse(request.prestige());
    assertEquals(1, request.position());
    assertFalse(request.countField());
  }

  @Test
  void parsesPrestigeCountWithOffset() {
    Request request = LeaderboardPlaceholder.parse("prestige_top_3_count");
    assertTrue(request.prestige());
    assertEquals(3, request.position());
    assertTrue(request.countField());
  }

  @Test
  void nonLeaderboardParamsReturnNull() {
    assertNull(LeaderboardPlaceholder.parse("player_rankups"));
    assertNull(LeaderboardPlaceholder.parse("rank_money_left"));
  }

  @Test
  void malformedOrNonNumericReturnNull() {
    assertNull(LeaderboardPlaceholder.parse("top_1"));        // missing field
    assertNull(LeaderboardPlaceholder.parse("top_x_name"));   // non-numeric position
    assertNull(LeaderboardPlaceholder.parse("prestige_top_2")); // missing field, prestige offset
  }

  // --- render ---------------------------------------------------------------------------------

  private final LeaderboardPlaceholder resolver = new LeaderboardPlaceholder("---", "0");

  @Test
  void rendersNameAndCountInRange() {
    List<LeaderboardEntry> top = List.of(entry("Steve", 42), entry("Alex", 17));
    assertEquals("Steve", resolver.render(LeaderboardPlaceholder.parse("top_1_name"), top));
    assertEquals("42", resolver.render(LeaderboardPlaceholder.parse("top_1_count"), top));
    assertEquals("Alex", resolver.render(LeaderboardPlaceholder.parse("top_2_name"), top));
  }

  @Test
  void emptySlotUsesFallback() {
    List<LeaderboardEntry> top = List.of(entry("Steve", 42));
    assertEquals("---", resolver.render(LeaderboardPlaceholder.parse("top_5_name"), top));
    assertEquals("0", resolver.render(LeaderboardPlaceholder.parse("top_5_count"), top));
  }

  @Test
  void positionZeroOrNegativeUsesFallback() {
    List<LeaderboardEntry> top = List.of(entry("Steve", 42));
    assertEquals("---", resolver.render(LeaderboardPlaceholder.parse("top_0_name"), top));
  }
}
