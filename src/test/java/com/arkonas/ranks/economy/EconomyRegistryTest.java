package com.arkonas.ranks.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

/** Pure selection logic of the multi-economy registry (no Bukkit needed). */
class EconomyRegistryTest {

  /** A no-op economy tagged for identity assertions. */
  private static Economy fake(String tag) {
    return new Economy() {
      @Override
      public double getBalance(Player player) {
        return 0;
      }

      @Override
      public void withdrawPlayer(Player player, double amount) {
      }

      @Override
      public void setPlayer(Player player, double amount) {
      }

      @Override
      public String toString() {
        return tag;
      }
    };
  }

  private EconomyRegistry registry() {
    return new EconomyRegistry(null);
  }

  @Test
  void selectsRequestedBackend() {
    Economy vault = fake("vault");
    Economy pp = fake("pp");
    EconomyRegistry registry = registry();
    registry.register("vault", arg -> vault);
    registry.register("playerpoints", arg -> pp);

    assertSame(vault, registry.select("vault"));
    assertSame(pp, registry.select("playerpoints"));
  }

  @Test
  void autoPrefersVault() {
    Economy vault = fake("vault");
    Economy pp = fake("pp");
    EconomyRegistry registry = registry();
    registry.register("playerpoints", arg -> pp);
    registry.register("vault", arg -> vault);

    assertSame(vault, registry.select("auto"));
    assertSame(vault, registry.select(null));
    assertSame(vault, registry.select(""));
  }

  @Test
  void autoFallsToOtherWhenNoVault() {
    Economy pp = fake("pp");
    EconomyRegistry registry = registry();
    registry.register("vault", arg -> null); // vault present but unavailable
    registry.register("playerpoints", arg -> pp);

    assertSame(pp, registry.select("auto"));
  }

  @Test
  void passesArgumentAfterColon() {
    AtomicReference<String> captured = new AtomicReference<>();
    Economy coins = fake("coins");
    EconomyRegistry registry = registry();
    registry.register("coinsengine", arg -> {
      captured.set(arg);
      return coins;
    });

    assertSame(coins, registry.select("coinsengine:gold"));
    assertEquals("gold", captured.get());
  }

  @Test
  void unknownOrUnavailableFallsBackToAuto() {
    Economy vault = fake("vault");
    EconomyRegistry registry = registry();
    registry.register("vault", arg -> vault);
    registry.register("playerpoints", arg -> null); // installed check fails

    assertSame(vault, registry.select("nonsense")); // unknown id -> auto -> vault
    assertSame(vault, registry.select("playerpoints")); // unavailable -> auto -> vault
  }

  @Test
  void throwingFactoryTreatedAsUnavailable() {
    Economy vault = fake("vault");
    EconomyRegistry registry = registry();
    registry.register("vault", arg -> vault);
    registry.register("boom", arg -> {
      throw new RuntimeException("kaboom");
    });

    assertSame(vault, registry.select("boom"));
  }

  @Test
  void nullWhenNothingAvailable() {
    EconomyRegistry registry = registry();
    registry.register("vault", arg -> null);
    assertNull(registry.select("auto"));
    assertNull(registry.select("vault"));
  }
}
