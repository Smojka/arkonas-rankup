package com.arkonas.ranks.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.rebirth.RebirthManager;

/** {@code /rebirths} — list the rebirth tiers and highlight the player's current one. */
@RequiredArgsConstructor
public class RebirthsCommand implements CommandExecutor {

  private final ArkonasRanksPlugin plugin;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (plugin.error(sender)) {
      return true;
    }
    RebirthManager rebirth = plugin.getRebirth();
    if (rebirth == null || !rebirth.isEnabled()) {
      sender.sendMessage(ChatColor.RED + "Rebirth is not enabled on this server.");
      return true;
    }
    int current = sender instanceof Player ? rebirth.currentIndex((Player) sender) : -1;
    sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Rebirth tiers");
    int index = 0;
    for (String group : rebirth.getGroups()) {
      boolean isCurrent = index == current;
      sender.sendMessage((isCurrent ? ChatColor.GREEN + "▶ " : ChatColor.GRAY + "  ")
          + group + (isCurrent ? ChatColor.DARK_GREEN + " (current)" : ""));
      index++;
    }
    return true;
  }
}
