package com.arkonas.ranks.economy;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * {@link Economy} backed by a named CoinsEngine currency, via reflection. The currency id is the
 * argument after the colon in the config value ({@code coinsengine:gold}). Method handles are
 * resolved up front; a missing plugin, unknown currency or differing API shape makes
 * {@link #tryCreate(String)} return null so the caller falls back to another backend.
 */
final class CoinsEngineEconomy implements Economy {

  private final Object currency;
  private final Method getBalance;
  private final Method removeBalance;
  private final Method setBalance;

  private CoinsEngineEconomy(Object currency, Method getBalance, Method removeBalance,
      Method setBalance) {
    this.currency = currency;
    this.getBalance = getBalance;
    this.removeBalance = removeBalance;
    this.setBalance = setBalance;
  }

  static Economy tryCreate(String currencyId) {
    if (currencyId == null || currencyId.isEmpty()
        || !Bukkit.getPluginManager().isPluginEnabled("CoinsEngine")) {
      return null;
    }
    try {
      Class<?> apiClass = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
      Object currency = apiClass.getMethod("getCurrency", String.class).invoke(null, currencyId);
      if (currency == null) {
        return null;
      }
      Class<?> currencyClass =
          Class.forName("su.nightexpress.coinsengine.api.currency.Currency");
      Method getBalance = apiClass.getMethod("getBalance", Player.class, currencyClass);
      Method removeBalance =
          apiClass.getMethod("removeBalance", Player.class, currencyClass, double.class);
      Method setBalance =
          apiClass.getMethod("setBalance", Player.class, currencyClass, double.class);
      return new CoinsEngineEconomy(currency, getBalance, removeBalance, setBalance);
    } catch (Throwable t) {
      return null;
    }
  }

  @Override
  public double getBalance(Player player) {
    try {
      Object value = getBalance.invoke(null, player, currency);
      return value instanceof Number ? ((Number) value).doubleValue() : 0;
    } catch (Throwable t) {
      return 0;
    }
  }

  @Override
  public void withdrawPlayer(Player player, double amount) {
    try {
      removeBalance.invoke(null, player, currency, amount);
    } catch (Throwable ignored) {
      // best effort
    }
  }

  @Override
  public void setPlayer(Player player, double amount) {
    try {
      setBalance.invoke(null, player, currency, amount);
    } catch (Throwable ignored) {
      // best effort
    }
  }
}
