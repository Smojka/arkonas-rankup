package com.arkonas.ranks.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MathExpressionTest {

  private static double eval(String expr) {
    return MathExpression.compile(expr).eval(Map.of());
  }

  private static double eval(String expr, Map<String, Double> vars) {
    return MathExpression.compile(expr).eval(vars);
  }

  @Test
  void arithmeticPrecedence() {
    assertEquals(7.0, eval("1 + 2 * 3"), 1e-9);
    assertEquals(9.0, eval("(1 + 2) * 3"), 1e-9);
    assertEquals(2.5, eval("10 / 4"), 1e-9);
    assertEquals(1.0, eval("10 % 3"), 1e-9);
  }

  @Test
  void powerIsRightAssociative() {
    assertEquals(512.0, eval("2 ^ 3 ^ 2"), 1e-9); // 2^(3^2) = 2^9
  }

  @Test
  void unaryMinus() {
    assertEquals(-4.0, eval("0 - 2 ^ 2"), 1e-9); // binary minus
    assertEquals(-4.0, eval("-(2 ^ 2)"), 1e-9);
    assertEquals(4.0, eval("(-2) ^ 2"), 1e-9);
    assertEquals(3.0, eval("--3"), 1e-9);
  }

  @Test
  void functions() {
    assertEquals(5.0, eval("max(3, 5)"), 1e-9);
    assertEquals(3.0, eval("min(3, 5)"), 1e-9);
    assertEquals(2.0, eval("floor(2.9)"), 1e-9);
    assertEquals(3.0, eval("ceil(2.1)"), 1e-9);
    assertEquals(3.0, eval("round(2.5)"), 1e-9);
    assertEquals(3.0, eval("sqrt(9)"), 1e-9);
    assertEquals(4.0, eval("abs(0 - 4)"), 1e-9);
    assertEquals(1024.0, eval("pow(2, 10)"), 1e-9);
  }

  @Test
  void variablesBareAndBraced() {
    assertEquals(1000.0, eval("1000 * 1.15 ^ index", Map.of("index", 0.0)), 1e-9);
    assertEquals(1150.0, eval("1000 * 1.15 ^ index", Map.of("index", 1.0)), 1e-9);
    assertEquals(200.0, eval("{prev} * 2", Map.of("prev", 100.0)), 1e-9);
  }

  @Test
  void divisionByZeroIsInfinite() {
    assertTrue(Double.isInfinite(eval("1 / 0")));
  }

  @Test
  void unknownVariableThrows() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> eval("x + 1"));
    assertTrue(e.getMessage().contains("Unknown variable"));
  }

  @Test
  void unknownFunctionThrows() {
    assertThrows(IllegalArgumentException.class, () -> eval("wat(1)"));
  }

  @Test
  void wrongArityThrows() {
    assertThrows(IllegalArgumentException.class, () -> eval("max(1)"));
    assertThrows(IllegalArgumentException.class, () -> eval("floor(1, 2)"));
  }

  @Test
  void malformedInputThrows() {
    assertThrows(IllegalArgumentException.class, () -> eval("1 2"));
    assertThrows(IllegalArgumentException.class, () -> eval("(1 + 2"));
    assertThrows(IllegalArgumentException.class, () -> MathExpression.compile(""));
    assertThrows(IllegalArgumentException.class, () -> MathExpression.compile("   "));
  }
}
