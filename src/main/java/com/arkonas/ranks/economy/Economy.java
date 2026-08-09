package com.arkonas.ranks.economy;

import org.bukkit.entity.Player;

public interface Economy {
    double getBalance(Player player);

    /**
     * Takes {@code amount} from the player.
     *
     * <p>Implementations must report the truth: returning true when nothing was actually withdrawn
     * hands out a free rankup, because the caller grants the rank on the strength of this result.
     *
     * @param player the player to charge
     * @param amount the amount to take
     * @return true only if the full amount was withdrawn
     */
    boolean withdrawPlayer(Player player, double amount);

    void setPlayer(Player player, double amount);
}
