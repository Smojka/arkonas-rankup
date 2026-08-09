package com.arkonas.ranks.ranksgui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class RanksGuiListener implements Listener {

  // keyed by uuid, not Player: holding Player instances kept every viewer who never closed the GUI
  // (or who quit with it open) alive in the map for the lifetime of the server
  private final Map<UUID, RanksGui> guiMap = new HashMap<>();

  @EventHandler
  public void on(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player)) {
      return;
    }
    RanksGui ranksGui = guiMap.get(player.getUniqueId());
    if (ranksGui != null
        && ranksGui.getInventory() != null
        && ranksGui.getInventory() == event.getInventory()) {
      guiMap.remove(player.getUniqueId());
    }
  }

  @EventHandler
  public void on(PlayerQuitEvent event) {
    guiMap.remove(event.getPlayer().getUniqueId());
  }

  /**
   * Cancels every click made while the ranks GUI is open, including clicks in the player's own
   * inventory: shift-click and hotbar/offhand swaps move items into the GUI, which is a virtual
   * inventory that discards them when it closes.
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void on(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    RanksGui ranksGui = guiMap.get(player.getUniqueId());
    if (ranksGui == null || event.getView().getTopInventory() != ranksGui.getInventory()) {
      return;
    }
    event.setCancelled(true);
    ranksGui.click(event);
  }

  /** Drags spanning the GUI are another way to push items into it, so they are cancelled too. */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void on(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    RanksGui ranksGui = guiMap.get(player.getUniqueId());
    if (ranksGui != null && event.getView().getTopInventory() == ranksGui.getInventory()) {
      event.setCancelled(true);
    }
  }

  public void open(RanksGui gui) {
    guiMap.put(gui.getPlayer().getUniqueId(), gui);
    gui.open();
  }
}
