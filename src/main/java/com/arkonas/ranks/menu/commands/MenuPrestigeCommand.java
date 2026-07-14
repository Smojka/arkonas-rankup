package com.arkonas.ranks.menu.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.menu.MenuModule;

/**
 * Menu wrapper for {@code /prestige}. Players get the prestige confirmation
 * menu; the console falls through to the wrapped chat executor.
 */
@RequiredArgsConstructor
public class MenuPrestigeCommand implements CommandExecutor {

  private final ArkonasRanksPlugin plugin;
  private final MenuModule menu;
  private final CommandExecutor chat;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      return chat.onCommand(sender, command, label, args);
    }
    Player player = (Player) sender;
    if (plugin.error(player)) {
      return true;
    }
    menu.openPrestige(player);
    return true;
  }
}
