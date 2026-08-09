package com.arkonas.ranks.economy;

import org.bukkit.entity.Player;

public class VaultEconomy implements Economy {
    private final net.milkbowl.vault.economy.Economy economy;

    public VaultEconomy(net.milkbowl.vault.economy.Economy economy) {
        this.economy = economy;
    }

    @Override
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean withdrawPlayer(Player player, double amount) {
        // Vault reports the outcome; the previous code discarded it, so a refused withdrawal (bank
        // plugin error, insufficient funds after a race) still let the rankup through for free
        net.milkbowl.vault.economy.EconomyResponse response =
            economy.withdrawPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    @Override
    public void setPlayer(Player player, double amount) {
        throw new UnsupportedOperationException();
    }

}
