package com.arkonas.ranks.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.prestige.Prestige;
import com.arkonas.ranks.ranks.RankElement;

/**
 * Called when a player ranks up from one prestige to another.
 */
public class PlayerPrestigeEvent extends PlayerEvent {
  private static final HandlerList handlers = new HandlerList();
  @Getter
  private final ArkonasRanksPlugin plugin;
  /**
   * The prestige someone is current at. Use <code>RankElement#getNext()</code> to get the prestige
   * a player is ranking up to.
   */
  @Getter
  private final RankElement<Prestige> prestige;

  public PlayerPrestigeEvent(ArkonasRanksPlugin plugin, @NotNull Player who, RankElement<Prestige> prestige) {
    super(who);
    this.plugin = plugin;
    this.prestige = prestige;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
