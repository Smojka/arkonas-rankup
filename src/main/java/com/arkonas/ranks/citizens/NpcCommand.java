package com.arkonas.ranks.citizens;

/**
 * Parses a single NPC action line into a dispatchable command. A line may be prefixed with
 * {@code [console]} (run from console) or {@code [player]} (run as the clicking player, the
 * default); {@code %player%} is substituted and any leading slash is stripped so the result feeds
 * straight into {@code performCommand}/{@code dispatchCommand}. Pure so it is unit-tested directly.
 */
public final class NpcCommand {

  /** A parsed line: run as console or as the player, with the placeholder already rendered. */
  public record Dispatch(boolean console, String command) {}

  private static final String CONSOLE = "[console]";
  private static final String PLAYER = "[player]";

  private NpcCommand() {
  }

  public static Dispatch parse(String line, String playerName) {
    String trimmed = line == null ? "" : line.trim();
    boolean console = false;
    if (trimmed.regionMatches(true, 0, CONSOLE, 0, CONSOLE.length())) {
      console = true;
      trimmed = trimmed.substring(CONSOLE.length()).trim();
    } else if (trimmed.regionMatches(true, 0, PLAYER, 0, PLAYER.length())) {
      trimmed = trimmed.substring(PLAYER.length()).trim();
    }
    String rendered = trimmed.replace("%player%", playerName == null ? "" : playerName);
    if (rendered.startsWith("/")) {
      rendered = rendered.substring(1);
    }
    return new Dispatch(console, rendered);
  }
}
