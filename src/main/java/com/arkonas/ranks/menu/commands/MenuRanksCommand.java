package com.arkonas.ranks.menu.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.menu.MenuModule;

/**
 * Menu wrapper for {@code /ranks}. Players get the rank path menu; the console
 * falls through to the wrapped chat executor (the chat listing).
 */
@RequiredArgsConstructor
public class MenuRanksCommand implements CommandExecutor {

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
    menu.openRankPath(player);
    return true;
  }
}
