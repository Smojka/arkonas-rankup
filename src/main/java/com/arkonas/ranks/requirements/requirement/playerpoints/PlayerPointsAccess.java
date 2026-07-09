package com.arkonas.ranks.requirements.requirement.playerpoints;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;

/**
 * Reads and deducts PlayerPoints balances by reflection, so the {@code playerpoints} requirement has
 * no PlayerPoints compile dependency. Handles are resolved up front; if the plugin is absent or the
 * API differs the accessor is unavailable and reads return 0 / deductions are no-ops (so the
 * requirement simply never passes rather than throwing).
 *
 * <p>Reflection-only: cannot be exercised by the test suite; validate on a live server.
 */
public final class PlayerPointsAccess {

  private final boolean available;
  private final Object api;
  private final Method look;
  private final Method take;

  private PlayerPointsAccess(boolean available, Object api, Method look, Method take) {
    this.available = available;
    this.api = api;
    this.look = look;
    this.take = take;
  }

  public static PlayerPointsAccess create() {
    if (!Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
      return new PlayerPointsAccess(false, null, null, null);
    }
    try {
      Class<?> playerPoints = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
      Object instance = playerPoints.getMethod("getInstance").invoke(null);
      Object api = playerPoints.getMethod("getAPI").invoke(instance);
      Method look = api.getClass().getMethod("look", UUID.class);
      Method take = api.getClass().getMethod("take", UUID.class, int.class);
      return new PlayerPointsAccess(true, api, look, take);
    } catch (Throwable t) {
      return new PlayerPointsAccess(false, null, null, null);
    }
  }

  public int look(UUID uuid) {
    if (!available) {
      return 0;
    }
    try {
      Object value = look.invoke(api, uuid);
      return value instanceof Number ? ((Number) value).intValue() : 0;
    } catch (Throwable t) {
      return 0;
    }
  }

  public void take(UUID uuid, int amount) {
    if (!available || amount <= 0) {
      return;
    }
    try {
      take.invoke(api, uuid, amount);
    } catch (Throwable ignored) {
      // best effort
    }
  }
}
