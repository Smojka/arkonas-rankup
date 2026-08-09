package com.arkonas.ranks.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

/**
 * The single listener for all menu screens. Cancels every click and drag while a
 * menu is open (including shift-clicks from the bottom inventory that would push
 * items in), routes in-menu clicks to the {@link AbstractMenu}, and cleans up on
 * close and quit.
 */
@RequiredArgsConstructor
public class MenuListener implements Listener {

  private final MenuModule module;

  // HIGHEST so a lower-priority plugin cannot un-cancel the event after this runs and let items be
  // moved into a menu (a virtual inventory, so anything moved in is destroyed when it closes)
  @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }
    Inventory top = event.getView().getTopInventory();
    if (!(top.getHolder() instanceof AbstractMenu menu)) {
      return;
    }
    // block all item movement (top clicks, bottom shift-clicks, hotbar swaps)
    event.setCancelled(true);

    Inventory clicked = event.getClickedInventory();
    if (clicked != null && clicked.equals(top)) {
      int slot = event.getSlot();
      if (slot >= 0 && slot < top.getSize()) {
        menu.onClick(slot, event.getClick());
      }
    }
  }

  @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
  public void onDrag(InventoryDragEvent event) {
    if (event.getView().getTopInventory().getHolder() instanceof AbstractMenu) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    if (event.getInventory().getHolder() instanceof AbstractMenu menu) {
      module.forget(menu);
      menu.closed();
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    module.forgetPlayer(event.getPlayer());
  }
}
