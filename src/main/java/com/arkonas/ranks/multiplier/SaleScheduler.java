package com.arkonas.ranks.multiplier;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Drives recurring cost sales: once a minute it reflects the {@link SaleSchedule}'s currently-active
 * window into the {@link MultiplierService} event slot, refreshing the expiry each tick so the sale
 * ends automatically when the window closes. A manual {@code /aru booster} always takes precedence
 * while it is active. The decision itself ({@link #apply}) is pure and unit-tested; this class only
 * supplies the server clock.
 */
public final class SaleScheduler extends BukkitRunnable {

  private static final long INTERVAL_TICKS = 1200L; // 60 seconds
  private static final long REFRESH_MILLIS = 70_000L; // interval + slack, so it never gaps

  private final SaleSchedule schedule;
  private final MultiplierService service;

  public SaleScheduler(SaleSchedule schedule, MultiplierService service) {
    this.schedule = schedule;
    this.service = service;
  }

  public void start(Plugin plugin) {
    runTaskTimer(plugin, 0L, INTERVAL_TICKS);
  }

  @Override
  public void run() {
    LocalDateTime now = LocalDateTime.now();
    apply(schedule, service, now.getDayOfWeek(), now.getHour() * 60 + now.getMinute(),
        System.currentTimeMillis(), REFRESH_MILLIS);
  }

  /**
   * Reflects the active sale window into the event slot, yielding to an active manual booster.
   * Pure so the precedence rules are testable.
   */
  static void apply(SaleSchedule schedule, MultiplierService service, DayOfWeek day, int minuteOfDay,
      long nowMillis, long refreshMillis) {
    if (service.isEventActive(nowMillis) && service.isManualEvent()) {
      return; // a manual /aru booster is running; do not clobber it
    }
    double factor = schedule.activeFactor(day, minuteOfDay);
    if (factor != 1.0) {
      service.setScheduledEvent(factor, nowMillis + refreshMillis);
    } else {
      service.setScheduledEvent(1.0, 0L); // no window active -> clear the scheduled slot
    }
  }
}
