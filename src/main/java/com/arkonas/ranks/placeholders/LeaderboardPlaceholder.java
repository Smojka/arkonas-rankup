package com.arkonas.ranks.placeholders;

import com.arkonas.ranks.data.LeaderboardEntry;
import java.util.List;

/**
 * Pure resolution of the leaderboard-position placeholders that feed hologram / TAB / scoreboard
 * display plugins — DecentHolograms, HolographicDisplays and TAB all resolve PlaceholderAPI in
 * their lines, so exposing these placeholders is a plugin-agnostic way to power a leaderboard
 * hologram without binding to any one plugin's API.
 *
 * <p>Supported params: {@code top_<n>_name}, {@code top_<n>_count},
 * {@code prestige_top_<n>_name}, {@code prestige_top_<n>_count}. Kept free of Bukkit so the parsing
 * and bounds handling are unit-tested directly.
 */
public final class LeaderboardPlaceholder {

  /** A parsed leaderboard request produced by {@link #parse(String)}. */
  public record Request(boolean prestige, int position, boolean countField) {}

  private final String emptyName;
  private final String emptyCount;

  public LeaderboardPlaceholder(String emptyName, String emptyCount) {
    this.emptyName = emptyName;
    this.emptyCount = emptyCount;
  }

  /** Parses a leaderboard placeholder, or null when {@code params} is not one of the family. */
  public static Request parse(String params) {
    boolean prestige = params.startsWith("prestige_top_");
    if (!prestige && !params.startsWith("top_")) {
      return null;
    }
    String[] parts = params.split("_");
    // parts[offset] = position, parts[offset + 1] = field ("name" | "count")
    int offset = prestige ? 2 : 1;
    if (parts.length < offset + 2) {
      return null;
    }
    int position;
    try {
      position = Integer.parseInt(parts[offset]);
    } catch (NumberFormatException e) {
      return null;
    }
    return new Request(prestige, position, parts[offset + 1].equals("count"));
  }

  /** Renders a parsed request against the leaderboard, using the empty-slot fallbacks. */
  public String render(Request request, List<LeaderboardEntry> entries) {
    if (request.position() < 1 || request.position() > entries.size()) {
      return request.countField() ? emptyCount : emptyName;
    }
    LeaderboardEntry entry = entries.get(request.position() - 1);
    return request.countField() ? String.valueOf(entry.count()) : entry.name();
  }
}
