package com.arkonas.ranks.economy;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * {@link Economy} backed by GemsEconomy, via reflection so the core never compiles against its API.
 * Method handles are resolved up front in {@link #tryCreate(String)}; a missing plugin or a differing
 * API shape makes creation return null so the caller falls back to another backend (RedisEconomy and
 * most others already register as a Vault economy and work through the {@code vault} backend).
 *
 * <p>Reflection-only: cannot be exercised by the test suite; validate on a live server.
 */
final class GemsEconomyEconomy implements Economy {

  private final Object api;
  private final Method getBalance;
  private final Method withdraw;
  private final Method setBalance;

  private GemsEconomyEconomy(Object api, Method getBalance, Method withdraw, Method setBalance) {
    this.api = api;
    this.getBalance = getBalance;
    this.withdraw = withdraw;
    this.setBalance = setBalance;
  }

  static Economy tryCreate(String arg) {
    if (!Bukkit.getPluginManager().isPluginEnabled("GemsEconomy")) {
      return null;
    }
    try {
      Class<?> apiClass = Class.forName("me.xanium.gemseconomy.api.GemsEconomyAPI");
      Object api = apiClass.getDeclaredConstructor().newInstance();
      Method getBalance = apiClass.getMethod("getBalance", UUID.class);
      Method withdraw = apiClass.getMethod("withdraw", UUID.class, double.class);
      Method setBalance = apiClass.getMethod("setBalance", UUID.class, double.class);
      return new GemsEconomyEconomy(api, getBalance, withdraw, setBalance);
    } catch (Throwable t) {
      return null;
    }
  }

  @Override
  public double getBalance(Player player) {
    try {
      Object value = getBalance.invoke(api, player.getUniqueId());
      return value instanceof Number ? ((Number) value).doubleValue() : 0;
    } catch (Throwable t) {
      return 0;
    }
  }

  @Override
  public boolean withdrawPlayer(Player player, double amount) {
    try {
      Object result = withdraw.invoke(api, player.getUniqueId(), amount);
      return !(result instanceof Boolean) || (Boolean) result;
    } catch (Throwable t) {
      // nothing was withdrawn, so the caller must not grant the rank
      return false;
    }
  }

  @Override
  public void setPlayer(Player player, double amount) {
    try {
      setBalance.invoke(api, player.getUniqueId(), amount);
    } catch (Throwable ignored) {
      // best effort
    }
  }
}
