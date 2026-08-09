package com.arkonas.ranks.economy;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * {@link Economy} backed by PlayerPoints, via reflection so the core never compiles against the
 * PlayerPoints API. All method handles are resolved up front in {@link #tryCreate()}; if the plugin
 * is absent or the API shape differs, creation returns null and the caller falls back to another
 * backend. Points are integers, so withdrawals round up (never undercharge the player).
 */
final class PlayerPointsEconomy implements Economy {

  private final Object api;
  private final Method look;
  private final Method take;
  private final Method set;

  private PlayerPointsEconomy(Object api, Method look, Method take, Method set) {
    this.api = api;
    this.look = look;
    this.take = take;
    this.set = set;
  }

  static Economy tryCreate() {
    if (!Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
      return null;
    }
    try {
      Class<?> playerPoints = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
      Object instance = playerPoints.getMethod("getInstance").invoke(null);
      Object api = playerPoints.getMethod("getAPI").invoke(instance);
      Class<?> apiClass = api.getClass();
      Method look = apiClass.getMethod("look", UUID.class);
      Method take = apiClass.getMethod("take", UUID.class, int.class);
      Method set = apiClass.getMethod("set", UUID.class, int.class);
      return new PlayerPointsEconomy(api, look, take, set);
    } catch (Throwable t) {
      return null;
    }
  }

  @Override
  public double getBalance(Player player) {
    try {
      Object value = look.invoke(api, player.getUniqueId());
      return value instanceof Number ? ((Number) value).doubleValue() : 0;
    } catch (Throwable t) {
      return 0;
    }
  }

  @Override
  public boolean withdrawPlayer(Player player, double amount) {
    try {
      Object result = take.invoke(api, player.getUniqueId(), (int) Math.ceil(amount));
      // the API returns a boolean; treat anything else as success only if the call did not throw
      return !(result instanceof Boolean) || (Boolean) result;
    } catch (Throwable t) {
      // a failed take leaves the balance unchanged, so the caller must not grant the rank
      return false;
    }
  }

  @Override
  public void setPlayer(Player player, double amount) {
    try {
      set.invoke(api, player.getUniqueId(), (int) amount);
    } catch (Throwable ignored) {
      // best effort
    }
  }
}
