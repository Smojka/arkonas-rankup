package com.arkonas.ranks.economy;

import com.arkonas.ranks.ArkonasRanksPlugin;

/**
 * The production {@link EconomyProvider}: builds an {@link EconomyRegistry} of the supported
 * backends and selects one from {@code economy.provider} in config (default {@code auto}, which
 * prefers Vault). Vault remains the default so existing setups are unchanged; PlayerPoints and
 * CoinsEngine are available via reflection when their plugin is installed.
 */
public final class ConfigurableEconomyProvider implements EconomyProvider {

  private final ArkonasRanksPlugin plugin;

  public ConfigurableEconomyProvider(ArkonasRanksPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public Economy getEconomy() {
    EconomyRegistry registry = new EconomyRegistry(plugin.getLogger());
    registry.register("vault", arg -> new VaultEconomyProvider().getEconomy());
    registry.register("playerpoints", arg -> PlayerPointsEconomy.tryCreate());
    registry.register("coinsengine", CoinsEngineEconomy::tryCreate);
    registry.register("gemseconomy", GemsEconomyEconomy::tryCreate);

    String requested = plugin.getConfig().getString("economy.provider", "auto");
    return registry.select(requested);
  }
}
