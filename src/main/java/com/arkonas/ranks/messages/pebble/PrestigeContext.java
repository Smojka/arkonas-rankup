package com.arkonas.ranks.messages.pebble;


import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.prestige.Prestige;

public class PrestigeContext extends RankContext {

  private final Prestige rank;

  public PrestigeContext(ArkonasRanksPlugin plugin, Player player, Prestige rank) {
    super(plugin, player, rank);
    this.rank = rank;
  }

  public String getFrom() {
    return rank.getFrom();
  }

  public String getTo() {
    return rank.getTo();
  }
}
