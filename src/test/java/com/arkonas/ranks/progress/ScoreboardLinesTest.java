package com.arkonas.ranks.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Pure scoreboard line rendering: placeholder substitution and the progress bar. */
class ScoreboardLinesTest {

  @Test
  void substitutesAllPlaceholders() {
    assertEquals("Steve: A -> B [####] 60%",
        ProgressDisplay.applyPlaceholders("%player%: %rank% -> %next% [%bar%] %percent%%",
            "Steve", "A", "B", 60, "####"));
  }

  @Test
  void progressBarFillsProportionally() {
    assertEquals("■■■■■□□□□□", ProgressDisplay.progressBar(0.5, 10, '■', '□'));
    assertEquals("□□□□□□□□□□", ProgressDisplay.progressBar(0.0, 10, '■', '□'));
    assertEquals("■■■■■■■■■■", ProgressDisplay.progressBar(1.0, 10, '■', '□'));
  }

  @Test
  void progressBarClampsOutOfRange() {
    assertEquals("■■■■■", ProgressDisplay.progressBar(2.0, 5, '■', '□'));
    assertEquals("□□□□□", ProgressDisplay.progressBar(-1.0, 5, '■', '□'));
  }
}
