package com.arkonas.ranks.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.rebirth.RebirthManager;

/** {@code /rebirth} — attempt a rebirth (see {@link RebirthManager}). */
@RequiredArgsConstructor
public class RebirthCommand implements CommandExecutor {

  private final ArkonasRanksPlugin plugin;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (plugin.error(sender)) {
      return true;
    }
    if (!(sender instanceof Player)) {
      return false;
    }
    RebirthManager rebirth = plugin.getRebirth();
    if (rebirth == null || !rebirth.isEnabled()) {
      sender.sendMessage(org.bukkit.ChatColor.RED + "Rebirth is not enabled on this server.");
      return true;
    }
    rebirth.rebirth((Player) sender);
    return true;
  }
}
