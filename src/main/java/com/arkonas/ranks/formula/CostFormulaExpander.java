package com.arkonas.ranks.formula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.arkonas.ranks.serialization.RankSerialized;

/**
 * Expands a {@link CostFormula} over a ladder of {@link RankSerialized ranks}, injecting a
 * generated requirement line (e.g. {@code money 1520.87}) into each rank based on its position in
 * the ladder. Side-effect-free: returns a new list, leaving the input untouched, so it is fully
 * unit-testable without a running server.
 *
 * <p>Ordering follows the {@code next} links from the ladder root; ranks not reachable from the
 * root (orphans / alternate branches) are appended in input order and still receive a cost keyed
 * by their appended index.</p>
 *
 * <p>Ranks that already declare the formula's requirement keep their explicit value unless
 * {@code override} is set. Ranks using prestige-style requirement maps are skipped entirely.</p>
 */
public final class CostFormulaExpander {

  private CostFormulaExpander() {
  }

  public static List<RankSerialized> expand(CostFormula formula, List<RankSerialized> input) {
    if (formula == null || !formula.isEnabled() || input == null || input.isEmpty()) {
      return input;
    }

    List<RankSerialized> ordered = order(input);
    int n = ordered.size();
    List<RankSerialized> output = new ArrayList<>(n);
    double prev = 0;

    for (int index = 0; index < n; index++) {
      RankSerialized rank = ordered.get(index);

      // Prestige-style requirement maps are out of scope for cost formulas.
      if (rank.getPrestigeRequirements() != null) {
        output.add(rank);
        continue;
      }

      List<String> requirements = rank.getRequirements();
      boolean hasExplicit = hasRequirement(requirements, formula.requirementName());

      if (hasExplicit && !formula.isOverride()) {
        output.add(rank);
        double explicit = readValue(requirements, formula.requirementName());
        if (!Double.isNaN(explicit)) {
          prev = explicit;
        }
        continue;
      }

      String line = formula.costLine(index, n, prev);
      List<String> merged = withRequirement(requirements, formula.requirementName(), line);
      output.add(copyWithRequirements(rank, merged));
      prev = formula.roundedValue(index, n, prev);
    }

    return output;
  }

  /** Orders ranks by following {@code next} links from the root, appending unreachable ranks. */
  private static List<RankSerialized> order(List<RankSerialized> input) {
    Map<String, RankSerialized> byName = new HashMap<>();
    Set<String> referencedAsNext = new LinkedHashSet<>();
    for (RankSerialized rank : input) {
      if (rank.getRank() != null) {
        byName.put(rank.getRank().toLowerCase(Locale.ROOT), rank);
      }
      if (rank.getNext() != null) {
        referencedAsNext.add(rank.getNext().toLowerCase(Locale.ROOT));
      }
    }

    RankSerialized root = null;
    for (RankSerialized rank : input) {
      if (rank.getRank() != null
          && !referencedAsNext.contains(rank.getRank().toLowerCase(Locale.ROOT))) {
        root = rank;
        break;
      }
    }

    List<RankSerialized> ordered = new ArrayList<>(input.size());
    Set<RankSerialized> seen = new LinkedHashSet<>();
    RankSerialized cursor = root;
    while (cursor != null && seen.add(cursor)) {
      ordered.add(cursor);
      String next = cursor.getNext();
      cursor = next == null ? null : byName.get(next.toLowerCase(Locale.ROOT));
    }
    // Append any ranks not reached from the root, preserving input order.
    for (RankSerialized rank : input) {
      if (!seen.contains(rank)) {
        ordered.add(rank);
      }
    }
    return ordered;
  }

  private static boolean hasRequirement(List<String> requirements, String name) {
    if (requirements == null) {
      return false;
    }
    for (String requirement : requirements) {
      if (nameOf(requirement).equalsIgnoreCase(name)) {
        return true;
      }
    }
    return false;
  }

  /** Reads the numeric value of the named requirement, or {@code NaN} if absent/unparseable. */
  private static double readValue(List<String> requirements, String name) {
    if (requirements == null) {
      return Double.NaN;
    }
    for (String requirement : requirements) {
      String[] parts = requirement.split(" ", 2);
      if (parts.length == 2 && parts[0].equalsIgnoreCase(name)) {
        try {
          return Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException ignored) {
          return Double.NaN;
        }
      }
    }
    return Double.NaN;
  }

  /** Returns requirements with the named line replaced (or appended if absent). */
  private static List<String> withRequirement(List<String> requirements, String name, String line) {
    List<String> merged = new ArrayList<>();
    boolean replaced = false;
    if (requirements != null) {
      for (String requirement : requirements) {
        if (nameOf(requirement).equalsIgnoreCase(name)) {
          merged.add(line);
          replaced = true;
        } else {
          merged.add(requirement);
        }
      }
    }
    if (!replaced) {
      merged.add(line);
    }
    return merged;
  }

  private static String nameOf(String requirement) {
    if (requirement == null) {
      return "";
    }
    int space = requirement.indexOf(' ');
    return space < 0 ? requirement : requirement.substring(0, space);
  }

  private static RankSerialized copyWithRequirements(RankSerialized rank, List<String> requirements) {
    RankSerialized copy = new RankSerialized(
        rank.getRank(),
        rank.getNext(),
        rank.getDisplayName(),
        rank.getCommands(),
        requirements,
        rank.getPrestigeRequirements(),
        rank.getMessages(),
        rank.getCostMultiplier());
    copy.setCelebration(rank.getCelebration());
    return copy;
  }
}
