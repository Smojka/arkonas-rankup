package com.arkonas.ranks.economy;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Selects an {@link Economy} backend by id from config, so money requirements can run on Vault,
 * PlayerPoints, CoinsEngine and others instead of Vault alone. Each backend is a {@link Factory}
 * that returns an {@link Economy} or null when its plugin is absent; selection falls back to
 * auto-detect (Vault first, then any available backend) when the requested one is missing.
 *
 * <p>The selection logic is Bukkit-free and unit-testable with fake factories; the concrete
 * factories (Vault + reflection adapters) are wired by {@link ConfigurableEconomyProvider}.</p>
 */
public final class EconomyRegistry {

  /** Creates an economy for an optional argument (the part after {@code ':'}, e.g. a currency). */
  @FunctionalInterface
  public interface Factory {
    Economy create(String arg);
  }

  private final Map<String, Factory> factories = new LinkedHashMap<>();
  private final Logger log;

  public EconomyRegistry(Logger log) {
    this.log = log;
  }

  public void register(String id, Factory factory) {
    factories.put(id.toLowerCase(Locale.ROOT), factory);
  }

  /**
   * Resolves the configured provider id (e.g. {@code vault}, {@code auto}, {@code coinsengine:gold})
   * to an economy, falling back to auto-detect when the requested backend is unknown or unavailable.
   *
   * @return the selected economy, or null when no backend is available
   */
  public Economy select(String requested) {
    String request = requested == null ? "auto" : requested.trim().toLowerCase(Locale.ROOT);
    if (request.isEmpty() || request.equals("auto")) {
      return auto();
    }

    int colon = request.indexOf(':');
    String base = colon < 0 ? request : request.substring(0, colon);
    String arg = colon < 0 ? null : request.substring(colon + 1);

    Factory factory = factories.get(base);
    if (factory == null) {
      warn("Unknown economy provider '" + base + "'; falling back to auto-detect.");
      return auto();
    }
    Economy economy = safe(factory, arg);
    if (economy != null) {
      return economy;
    }
    warn("Economy provider '" + request + "' is unavailable; falling back to auto-detect.");
    return auto();
  }

  private Economy auto() {
    Factory vault = factories.get("vault");
    if (vault != null) {
      Economy economy = safe(vault, null);
      if (economy != null) {
        return economy;
      }
    }
    for (Map.Entry<String, Factory> entry : factories.entrySet()) {
      if (entry.getKey().equals("vault")) {
        continue;
      }
      Economy economy = safe(entry.getValue(), null);
      if (economy != null) {
        return economy;
      }
    }
    return null;
  }

  private Economy safe(Factory factory, String arg) {
    try {
      return factory.create(arg);
    } catch (Throwable t) {
      return null;
    }
  }

  private void warn(String message) {
    if (log != null) {
      log.warning(message);
    }
  }
}
