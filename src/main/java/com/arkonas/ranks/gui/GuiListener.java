package com.arkonas.ranks.gui;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import com.arkonas.ranks.ArkonasRanksPlugin;

@RequiredArgsConstructor
public class GuiListener implements Listener {
  private final ArkonasRanksPlugin plugin;

  /**
   * Cancels every click made while a confirmation GUI is open — including clicks in the player's own
   * inventory, which is where shift-click, hotbar swap and offhand swap push items <em>into</em> the
   * GUI. The GUI is a virtual inventory that is never given back, so anything moved in is destroyed
   * when it closes. Only clicks on the GUI itself are routed to the buttons.
   *
   * <p>Priority is HIGHEST (and it ignores nothing) so a lower-priority plugin cannot un-cancel the
   * event after this runs.
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void on(InventoryClickEvent e) {
    Inventory top = e.getView().getTopInventory();
    if (!(top.getHolder() instanceof Gui gui)) {
      return;
    }
    e.setCancelled(true);

    if (!(e.getWhoClicked() instanceof Player player)) {
      return;
    }
    Inventory clicked = e.getClickedInventory();
    if (clicked == null || !clicked.equals(top)) {
      return; // click in the player's own inventory: cancelled above, no button behind it
    }

    int slot = e.getSlot();
    if (slot < 0 || slot >= top.getSize()) {
      return;
    }

    if (gui.getNr(slot) == Gui.RANKUP_NR) {
      Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
      if (gui.isPrestige()) {
        plugin.getHelper().prestige(player);
      } else {
        plugin.getHelper().rankup(player);
      }
    } else if (gui.getNr(slot) == Gui.CANCEL_NR) {
      Bukkit.getScheduler().runTask(plugin, () -> {
        player.closeInventory();
        if (gui.isReturnToRanksGui()) {
          Bukkit.dispatchCommand(player, "ranks");
        }
      });
    }
  }

  /** Drags spanning the GUI are another way to push items into it, so they are cancelled too. */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void on(InventoryDragEvent e) {
    if (e.getView().getTopInventory().getHolder() instanceof Gui) {
      e.setCancelled(true);
    }
  }
}
