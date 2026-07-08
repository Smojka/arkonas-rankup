package com.arkonas.ranks.formula;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Generates a rank requirement line (for example {@code "money 1520.87"}) from a formula, so an
 * admin can define a single expression instead of hand-authoring a cost on every rank.
 *
 * <p>The expression is evaluated with these variables:</p>
 * <ul>
 *   <li>{@code index} — 0-based position of the rank in its ladder</li>
 *   <li>{@code n} — total number of ranks in the ladder</li>
 *   <li>{@code prev} — the cost generated for the previous rank (0 for the first)</li>
 * </ul>
 *
 * <p>The core logic is Bukkit-free and unit-testable via the primitive constructor;
 * {@link #fromConfig(ConfigurationSection)} adapts a config section.</p>
 */
public final class CostFormula {

  private final boolean enabled;
  private final String requirementName;
  private final MathExpression expression;
  private final int round;
  private final boolean override;

  public CostFormula(boolean enabled, String requirementName, MathExpression expression, int round,
      boolean override) {
    this.enabled = enabled;
    this.requirementName = requirementName;
    this.expression = expression;
    this.round = round;
    this.override = override;
  }

  /** A disabled formula that generates nothing. */
  public static CostFormula disabled() {
    return new CostFormula(false, "money", null, -1, false);
  }

  /**
   * Builds a formula from a config section, or a {@link #disabled()} formula if the section is
   * null or {@code enabled} is not true.
   *
   * @throws IllegalArgumentException if enabled but the expression is missing or invalid
   */
  public static CostFormula fromConfig(ConfigurationSection section) {
    if (section == null || !section.getBoolean("enabled", false)) {
      return disabled();
    }
    String requirement = section.getString("requirement", "money");
    String expr = section.getString("expression");
    if (expr == null || expr.isBlank()) {
      throw new IllegalArgumentException(
          "cost-formula is enabled but has no 'expression'");
    }
    int round = section.getInt("round", 2);
    boolean override = section.getBoolean("override", false);
    return new CostFormula(true, requirement, MathExpression.compile(expr), round, override);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isOverride() {
    return override;
  }

  public String requirementName() {
    return requirementName;
  }

  /** Evaluates the formula to a raw (unrounded) numeric cost. */
  public double value(int index, int n, double prev) {
    Map<String, Double> vars = new HashMap<>();
    vars.put("index", (double) index);
    vars.put("n", (double) n);
    vars.put("prev", prev);
    double result = expression.eval(vars);
    if (!Double.isFinite(result)) {
      throw new IllegalArgumentException(
          "cost-formula produced a non-finite value (" + result + ") at index " + index
              + "; check for division by zero or overflow");
    }
    return result;
  }

  /** The rounded numeric cost that {@link #costLine(int, int, double)} would emit. */
  public double roundedValue(int index, int n, double prev) {
    double raw = value(index, n, prev);
    if (round < 0) {
      return raw;
    }
    return BigDecimal.valueOf(raw).setScale(round, RoundingMode.HALF_UP).doubleValue();
  }

  /**
   * Produces the requirement line for a rank, e.g. {@code "money 1520.87"}. The numeric part is
   * always plain decimal (never scientific notation) so it round-trips through
   * {@code Double.parseDouble}.
   */
  public String costLine(int index, int n, double prev) {
    double raw = value(index, n, prev);
    BigDecimal decimal = BigDecimal.valueOf(raw);
    if (round >= 0) {
      decimal = decimal.setScale(round, RoundingMode.HALF_UP);
    }
    return requirementName + " " + decimal.stripTrailingZeros().toPlainString();
  }
}
