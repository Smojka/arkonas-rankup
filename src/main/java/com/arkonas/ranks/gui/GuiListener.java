package com.arkonas.ranks.gui;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import com.arkonas.ranks.ArkonasRanksPlugin;

@RequiredArgsConstructor
public class GuiListener implements Listener {
  private final ArkonasRanksPlugin plugin;

  @EventHandler
  public void on(InventoryClickEvent e) {
    Inventory inventory = e.getInventory();
    if (inventory == null
        || !(inventory.getHolder() instanceof Gui)
        || !inventory.equals(e.getClickedInventory())) {
      return;
    }
    e.setCancelled(true);

    Player player = (Player) e.getWhoClicked();
    Gui gui = (Gui) inventory.getHolder();

    if (gui.getNr(e.getSlot()) == Gui.RANKUP_NR) {
      Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
      if (gui.isPrestige()) {
        plugin.getHelper().prestige(player);
      } else {
        plugin.getHelper().rankup(player);
      }
    } else if (gui.getNr(e.getSlot()) == Gui.CANCEL_NR) {
      Bukkit.getScheduler().runTask(plugin, () -> {
        player.closeInventory();
        if (gui.isReturnToRanksGui()) {
          Bukkit.dispatchCommand(player, "ranks");
        }
      });
    }
  }
}
