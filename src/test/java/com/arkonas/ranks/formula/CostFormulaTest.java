package com.arkonas.ranks.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.arkonas.ranks.serialization.RankSerialized;
import org.junit.jupiter.api.Test;

class CostFormulaTest {

  private static CostFormula power(int round, boolean override) {
    return new CostFormula(true, "money", MathExpression.compile("100 * 2 ^ {index}"), round,
        override);
  }

  private static RankSerialized rank(String name, String next, List<String> requirements) {
    return new RankSerialized(name, next, name, new ArrayList<>(), requirements, null,
        new HashMap<>());
  }

  private static List<String> reqs(String... lines) {
    return new ArrayList<>(List.of(lines));
  }

  // --- CostFormula ---------------------------------------------------------------------------

  @Test
  void costLineStripsTrailingZeros() {
    CostFormula f = new CostFormula(true, "money",
        MathExpression.compile("1000 * 1.15 ^ {index}"), 2, false);
    assertEquals("money 1000", f.costLine(0, 10, 0));
    assertEquals("money 1150", f.costLine(1, 10, 0));
    assertEquals("money 1322.5", f.costLine(2, 10, 0));
  }

  @Test
  void roundingApplied() {
    CostFormula f = new CostFormula(true, "money",
        MathExpression.compile("100 / 3"), 2, false);
    assertEquals("money 33.33", f.costLine(0, 1, 0));
    assertEquals(33.33, f.roundedValue(0, 1, 0), 1e-9);
  }

  @Test
  void noRoundingWhenNegative() {
    CostFormula f = new CostFormula(true, "money", MathExpression.compile("100 / 3"), -1, false);
    assertEquals(100.0 / 3.0, f.roundedValue(0, 1, 0), 1e-9);
  }

  @Test
  void disabledFormula() {
    assertFalse(CostFormula.disabled().isEnabled());
  }

  // --- Expander ------------------------------------------------------------------------------

  @Test
  void expandsLinearLadderByPosition() {
    List<RankSerialized> input = List.of(
        rank("A", "B", reqs()),
        rank("B", "C", reqs()),
        rank("C", "D", reqs()));
    List<RankSerialized> out = CostFormulaExpander.expand(power(0, false), input);

    assertEquals("money 100", moneyOf(out, "A"));
    assertEquals("money 200", moneyOf(out, "B"));
    assertEquals("money 400", moneyOf(out, "C"));
  }

  @Test
  void explicitCostWinsWhenNotOverriding() {
    List<RankSerialized> input = List.of(
        rank("A", "B", reqs()),
        rank("B", "C", reqs("money 999")),
        rank("C", "D", reqs()));
    List<RankSerialized> out = CostFormulaExpander.expand(power(0, false), input);

    assertEquals("money 100", moneyOf(out, "A"));
    assertEquals("money 999", moneyOf(out, "B")); // untouched
    assertEquals("money 400", moneyOf(out, "C")); // index-based, unaffected
  }

  @Test
  void overrideReplacesExplicitCost() {
    List<RankSerialized> input = List.of(
        rank("A", "B", reqs()),
        rank("B", "C", reqs("money 999")),
        rank("C", "D", reqs()));
    List<RankSerialized> out = CostFormulaExpander.expand(power(0, true), input);

    assertEquals("money 200", moneyOf(out, "B"));
  }

  @Test
  void preservesOtherRequirements() {
    List<RankSerialized> input = List.of(
        rank("A", "B", reqs("playtime-minutes 5", "permission foo")));
    List<RankSerialized> out = CostFormulaExpander.expand(power(0, false), input);

    List<String> a = find(out, "A").getRequirements();
    assertTrue(a.contains("playtime-minutes 5"));
    assertTrue(a.contains("permission foo"));
    assertTrue(a.contains("money 100"));
  }

  @Test
  void prevVariableChainsAcrossRanks() {
    CostFormula f = new CostFormula(true, "money",
        MathExpression.compile("max(100, {prev} * 2)"), 0, false);
    List<RankSerialized> input = List.of(
        rank("A", "B", reqs()),
        rank("B", "C", reqs()),
        rank("C", "D", reqs()));
    List<RankSerialized> out = CostFormulaExpander.expand(f, input);

    assertEquals("money 100", moneyOf(out, "A"));
    assertEquals("money 200", moneyOf(out, "B"));
    assertEquals("money 400", moneyOf(out, "C"));
  }

  @Test
  void skipsPrestigeStyleRequirementMaps() {
    RankSerialized prestige = new RankSerialized("P", "Q", "P", new ArrayList<>(), null,
        new HashMap<>(), new HashMap<>());
    List<RankSerialized> out =
        CostFormulaExpander.expand(power(0, false), List.of(prestige));
    // Unchanged, no money injected, no crash.
    assertSame(null, out.get(0).getRequirements());
  }

  @Test
  void disabledFormulaReturnsInputUnchanged() {
    List<RankSerialized> input = List.of(rank("A", "B", reqs()));
    assertSame(input, CostFormulaExpander.expand(CostFormula.disabled(), input));
  }

  private static String moneyOf(List<RankSerialized> ranks, String name) {
    for (String requirement : find(ranks, name).getRequirements()) {
      if (requirement.startsWith("money ")) {
        return requirement;
      }
    }
    return null;
  }

  private static RankSerialized find(List<RankSerialized> ranks, String name) {
    return ranks.stream().filter(r -> name.equals(r.getRank())).findFirst().orElseThrow();
  }
}
