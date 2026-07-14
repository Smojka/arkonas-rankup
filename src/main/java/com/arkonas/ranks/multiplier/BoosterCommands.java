package com.arkonas.ranks.multiplier;

import java.util.Locale;

/**
 * Pure logic for the {@code /aru booster} admin command: start a temporary server-wide cost
 * booster (a sale/event), clear it, or report its status. Kept free of Bukkit so parsing, duration
 * handling and the messages are unit-tested directly; {@code InfoCommand} is a thin adapter that
 * passes {@code System.currentTimeMillis()} in and prints the returned message.
 *
 * <p>Usage: {@code booster <factor> <duration>} (e.g. {@code booster 0.5 30m} = half-price rankups
 * for 30 minutes), {@code booster clear}, or {@code booster status}.
 */
public final class BoosterCommands {

  /** Outcome of handling the command: whether it succeeded and the message to show the sender. */
  public record Result(boolean success, String message) {}

  private BoosterCommands() {
  }

  /**
   * @param sub the args after "booster" (e.g. {@code ["0.5","30m"]}, {@code ["clear"]}, {@code []})
   * @param nowMillis current epoch millis (injected for testability)
   */
  public static Result handle(MultiplierService service, String[] sub, long nowMillis) {
    if (sub.length == 0 || sub[0].equalsIgnoreCase("status")) {
      if (service.isEventActive(nowMillis)) {
        long remaining = (service.eventUntilMillis() - nowMillis) / 1000L;
        return new Result(true, "Active booster: x" + trim(service.eventFactor()) + " cost, "
            + formatDuration(remaining) + " remaining.");
      }
      return new Result(true, "No booster active.");
    }

    String action = sub[0];
    if (action.equalsIgnoreCase("clear") || action.equalsIgnoreCase("stop")
        || action.equalsIgnoreCase("off")) {
      service.setEventMultiplier(1.0, 0L);
      return new Result(true, "Booster cleared.");
    }

    if (sub.length < 2) {
      return new Result(false, "Usage: booster <factor> <duration> | clear | status");
    }
    Double factor = parseFactor(action);
    if (factor == null) {
      return new Result(false,
          "Invalid factor '" + action + "'. Use a positive number, e.g. 0.5 for half price.");
    }
    long seconds = parseDurationSeconds(sub[1]);
    if (seconds < 0) {
      return new Result(false, "Invalid duration '" + sub[1] + "'. Use e.g. 30m, 2h, 600s.");
    }
    service.setEventMultiplier(factor, nowMillis + seconds * 1000L);
    return new Result(true,
        "Booster set: x" + trim(factor) + " cost for " + formatDuration(seconds) + ".");
  }

  /** Parses a positive cost factor, or null if invalid. */
  static Double parseFactor(String value) {
    try {
      double factor = Double.parseDouble(value.trim());
      return factor > 0 ? factor : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** Parses a duration ({@code 600}, {@code 600s}, {@code 30m}, {@code 2h}, {@code 1d}) to seconds,
   *  or -1 if invalid. A bare number is seconds. */
  static long parseDurationSeconds(String value) {
    if (value == null || value.isBlank()) {
      return -1;
    }
    String text = value.trim().toLowerCase(Locale.ROOT);
    char suffix = text.charAt(text.length() - 1);
    long multiplier;
    switch (suffix) {
      case 's' -> multiplier = 1L;
      case 'm' -> multiplier = 60L;
      case 'h' -> multiplier = 3600L;
      case 'd' -> multiplier = 86400L;
      default -> {
        if (!Character.isDigit(suffix)) {
          return -1; // unknown unit
        }
        multiplier = 1L;
      }
    }
    String number = Character.isDigit(suffix) ? text : text.substring(0, text.length() - 1);
    try {
      long amount = Long.parseLong(number.trim());
      return amount > 0 ? amount * multiplier : -1;
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /** Human-readable duration, largest units first (e.g. {@code 1h 30m}). */
  static String formatDuration(long seconds) {
    if (seconds <= 0) {
      return "0s";
    }
    long days = seconds / 86400;
    long hours = (seconds % 86400) / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    StringBuilder builder = new StringBuilder();
    if (days > 0) {
      builder.append(days).append("d ");
    }
    if (hours > 0) {
      builder.append(hours).append("h ");
    }
    if (minutes > 0) {
      builder.append(minutes).append("m ");
    }
    if (secs > 0) {
      builder.append(secs).append("s ");
    }
    return builder.toString().trim();
  }

  /** Drops a trailing {@code .0} so {@code 2.0} reads as {@code 2}. */
  private static String trim(double value) {
    if (value == Math.floor(value) && !Double.isInfinite(value)) {
      return String.valueOf((long) value);
    }
    return String.valueOf(value);
  }
}
