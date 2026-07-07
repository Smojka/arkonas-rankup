package com.arkonas.ranks.providers;

import com.arkonas.ranks.economy.Economy;
import com.arkonas.ranks.economy.EconomyProvider;

public class TestEconomyProvider implements EconomyProvider {
    @Override
    public Economy getEconomy() {
        return new TestEconomy();
    }
}
