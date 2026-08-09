package com.arkonas.ranks.providers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.arkonas.ranks.economy.Economy;

public class TestEconomy implements Economy {
    private final Map<UUID, Double> balances = new HashMap<>();

    @Override
    public double getBalance(Player player) {
        return balances.getOrDefault(player.getUniqueId(), 0D);
    }

    @Override
    public boolean withdrawPlayer(Player player, double amount) {
        double balance = balances.getOrDefault(player.getUniqueId(), 0D);
        if (balance < amount) {
            return false;
        }
        balances.put(player.getUniqueId(), balance - amount);
        return true;
    }

    public void setPlayer(Player player, double amount) {
        balances.put(player.getUniqueId(), amount);
    }
}
