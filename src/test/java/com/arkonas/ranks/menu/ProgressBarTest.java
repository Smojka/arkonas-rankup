package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pure tests for the smooth-block and MiniMessage-gradient bar renderers. */
class ProgressBarTest {

  private final ProgressBar bar = new ProgressBar("▰", "▱", 4);

  @Test
  void smoothBarEndpoints() {
    assertEquals("    ", bar.smoothBar(0));
    assertEquals("████", bar.smoothBar(1));
  }

  @Test
  void smoothBarHalf() {
    assertEquals("██  ", bar.smoothBar(0.5));
  }

  @Test
  void smoothBarSubSegment() {
    // 1/8 of the whole bar = one eighth of the first cell
    assertEquals("▏   ", bar.smoothBar(1.0 / 32.0));
  }

  @Test
  void gradientBarWrapsFilledPortion() {
    String out = bar.gradientBar(0.5, "#55ff55", "#00aa00", "#555555");
    assertTrue(out.contains("<gradient:#55ff55:#00aa00>"), out);
    assertTrue(out.contains("</gradient>"), out);
    assertTrue(out.contains("▰▰"), out);
    assertTrue(out.contains("<#555555>▱▱"), out);
  }

  @Test
  void gradientBarFullHasNoEmptyPortion() {
    String out = bar.gradientBar(1.0, "#55ff55", "#00aa00", "#555555");
    assertTrue(out.contains("<gradient:"), out);
    assertFalse(out.contains("<#555555>"), out);
  }

  @Test
  void gradientBarNormalisesHexWithoutHash() {
    String out = bar.gradientBar(1.0, "55ff55", "00aa00", "555555");
    assertTrue(out.contains("<gradient:#55ff55:#00aa00>"), out);
  }

  @Test
  void gradientPartialBySegmentCount() {
    // 2 of 4 filled, independent of any fraction
    String out = bar.gradientPartial(2, "#55ff55", "#00aa00", "#555555");
    assertTrue(out.contains("<gradient:#55ff55:#00aa00>▰▰</gradient>"), out);
    assertTrue(out.contains("<#555555>▱▱"), out);
  }

  @Test
  void gradientPartialClampsAndEndpoints() {
    assertFalse(bar.gradientPartial(4, "#a", "#b", "#c").contains("<#"), "full = no empty tail");
    assertFalse(bar.gradientPartial(0, "#a", "#b", "#c").contains("<gradient"), "empty = no gradient");
    // clamps out-of-range segment counts
    assertEquals(bar.gradientPartial(4, "#a", "#b", "#c"), bar.gradientPartial(99, "#a", "#b", "#c"));
  }

  @Test
  void styledDispatch() {
    assertEquals("▰▰▱▱", bar.styled("classic", 2, 0.5, "#a", "#b", "#c"));
    assertEquals("██  ", bar.styled("smooth", 2, 0.5, "#a", "#b", "#c"));
    assertTrue(bar.styled("gradient", 2, 0.5, "#a", "#b", "#c").contains("<gradient:#a:#b>"));
    // unknown / null style falls back to classic
    assertEquals("▰▰▱▱", bar.styled("wat", 2, 0.5, "#a", "#b", "#c"));
    assertEquals("▰▰▱▱", bar.styled(null, 2, 0.5, "#a", "#b", "#c"));
  }
}
