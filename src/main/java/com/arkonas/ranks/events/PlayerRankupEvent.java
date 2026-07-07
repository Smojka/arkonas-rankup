package com.arkonas.ranks.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;

/**
 * Called when a player ranks up from one rank to another.
 */
public class PlayerRankupEvent extends PlayerEvent {
  private static final HandlerList handlers = new HandlerList();
  @Getter
  private final ArkonasRanksPlugin plugin;
  /**
   * The rank someone is current at. Use <code>RankElement#getNext()</code> to get the rank
   * a player is ranking up to.
   */
  @Getter
  private final RankElement<Rank> rank;

  public PlayerRankupEvent(ArkonasRanksPlugin plugin, @NotNull Player who, RankElement<Rank> rank) {
    super(who);
    this.plugin = plugin;
    this.rank = rank;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
