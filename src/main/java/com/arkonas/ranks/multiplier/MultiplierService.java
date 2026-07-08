package com.arkonas.ranks.multiplier;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Computes a cost multiplier applied to money requirements, so servers can offer rank-up discounts
 * (factor &lt; 1) or surcharges (factor &gt; 1). The factor combines:
 * <ul>
 *   <li>a global factor from config;</li>
 *   <li>the best (lowest) permission-based factor the player has, e.g. a VIP discount;</li>
 *   <li>a temporary server-wide event booster with an expiry.</li>
 * </ul>
 * A default (no config) service returns 1.0, leaving costs unchanged.
 */
public final class MultiplierService {

  private final double global;
  private final Map<String, Double> permissionFactors;
  private double eventFactor = 1.0;
  private long eventUntilMillis = 0L;

  public MultiplierService(double global, Map<String, Double> permissionFactors) {
    this.global = global;
    this.permissionFactors = permissionFactors;
  }

  public static MultiplierService disabled() {
    return new MultiplierService(1.0, new LinkedHashMap<>());
  }

  public static MultiplierService fromConfig(ConfigurationSection section) {
    if (section == null) {
      return disabled();
    }
    double global = section.getDouble("global", 1.0);
    Map<String, Double> permissions = new LinkedHashMap<>();
    ConfigurationSection permSection = section.getConfigurationSection("permissions");
    if (permSection != null) {
      for (String permission : permSection.getKeys(false)) {
        permissions.put(permission, permSection.getDouble(permission, 1.0));
      }
    }
    return new MultiplierService(global, permissions);
  }

  /**
   * Sets a temporary server-wide multiplier active until {@code untilMillis} (epoch). Used by an
   * event/booster command.
   */
  public void setEventMultiplier(double factor, long untilMillis) {
    this.eventFactor = factor;
    this.eventUntilMillis = untilMillis;
  }

  public boolean isEventActive(long nowMillis) {
    return nowMillis < eventUntilMillis;
  }

  /** The active event booster factor (only meaningful while {@link #isEventActive} is true). */
  public double eventFactor() {
    return eventFactor;
  }

  /** Epoch millis the event booster runs until (0 = none). */
  public long eventUntilMillis() {
    return eventUntilMillis;
  }

  /** The cost multiplier for a player (1.0 = unchanged). Never negative. */
  public double costFactor(Player player) {
    double factor = global;

    double best = 1.0;
    boolean matched = false;
    for (Map.Entry<String, Double> entry : permissionFactors.entrySet()) {
      if (player.hasPermission(entry.getKey())) {
        best = matched ? Math.min(best, entry.getValue()) : entry.getValue();
        matched = true;
      }
    }
    if (matched) {
      factor *= best;
    }

    if (isEventActive(System.currentTimeMillis())) {
      factor *= eventFactor;
    }

    return Math.max(0, factor);
  }
}
