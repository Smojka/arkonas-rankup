package com.arkonas.ranks.multiplier;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Recurring cost-sale windows (e.g. "half price on weekends 18:00–20:00"). Pure: it maps a
 * (day-of-week, minute-of-day) to the best active sale factor, so the schedule logic is unit-tested
 * without Bukkit. The {@link SaleScheduler} runnable feeds it the server clock and drives
 * {@link MultiplierService#setScheduledEvent}.
 */
public final class SaleSchedule {

  /** One recurring window: on {@code days}, from {@code startMinute} (incl) to {@code endMinute}
   *  (excl) minutes-of-day, apply {@code factor}. */
  public record Window(Set<DayOfWeek> days, int startMinute, int endMinute, double factor) {
    boolean covers(DayOfWeek day, int minute) {
      return days.contains(day) && minute >= startMinute && minute < endMinute;
    }
  }

  private final List<Window> windows;

  public SaleSchedule(List<Window> windows) {
    this.windows = windows;
  }

  public boolean isEmpty() {
    return windows.isEmpty();
  }

  /** The best (lowest = biggest discount) factor of the windows covering this moment, or 1.0. */
  public double activeFactor(DayOfWeek day, int minuteOfDay) {
    double best = 1.0;
    boolean matched = false;
    for (Window window : windows) {
      if (window.covers(day, minuteOfDay)) {
        best = matched ? Math.min(best, window.factor()) : window.factor();
        matched = true;
      }
    }
    return matched ? best : 1.0;
  }

  public static SaleSchedule fromConfig(ConfigurationSection boosters) {
    if (boosters == null) {
      return new SaleSchedule(List.of());
    }
    return fromMaps(boosters.getMapList("schedule"));
  }

  /** Builds a schedule from a list of raw config maps, skipping any malformed entry. */
  static SaleSchedule fromMaps(List<Map<?, ?>> raw) {
    List<Window> windows = new ArrayList<>();
    for (Map<?, ?> entry : raw) {
      Window window = parseWindow(entry);
      if (window != null) {
        windows.add(window);
      }
    }
    return new SaleSchedule(windows);
  }

  private static Window parseWindow(Map<?, ?> entry) {
    Set<DayOfWeek> days = parseDays(entry.get("days"));
    int start = parseTime(String.valueOf(entry.get("start")));
    int end = parseTime(String.valueOf(entry.get("end")));
    Double factor = parseFactor(entry.get("factor"));
    if (days.isEmpty() || start < 0 || end < 0 || start >= end || factor == null) {
      return null; // skip malformed windows rather than fail the whole schedule
    }
    return new Window(days, start, end, factor);
  }

  private static Set<DayOfWeek> parseDays(Object value) {
    EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
    if (!(value instanceof Iterable<?> iterable)) {
      return days;
    }
    for (Object element : iterable) {
      String token = String.valueOf(element).trim().toUpperCase(Locale.ROOT);
      if (token.equals("ALL") || token.equals("*") || token.equals("EVERYDAY")) {
        return EnumSet.allOf(DayOfWeek.class);
      }
      try {
        days.add(DayOfWeek.valueOf(token));
      } catch (IllegalArgumentException ignored) {
        // skip unknown day token
      }
    }
    return days;
  }

  /** Parses {@code HH:mm} to minute-of-day, or -1 if invalid. */
  static int parseTime(String text) {
    if (text == null) {
      return -1;
    }
    String[] parts = text.trim().split(":");
    if (parts.length != 2) {
      return -1;
    }
    try {
      int hour = Integer.parseInt(parts[0].trim());
      int minute = Integer.parseInt(parts[1].trim());
      if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
        return -1;
      }
      return hour * 60 + minute;
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private static Double parseFactor(Object value) {
    if (value == null) {
      return null;
    }
    try {
      double factor = Double.parseDouble(String.valueOf(value).trim());
      return factor > 0 ? factor : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
