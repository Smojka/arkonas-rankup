package com.arkonas.ranks.menu.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;

/**
 * Menu wrapper for {@code /rankup}. A player with a next rank opens the rankup
 * screen; {@code /rankup top} opens the leaderboard. The console, the
 * {@code noconfirm} sub-command and the NOT_IN_LADDER case all fall through to
 * the wrapped parity executor so their behaviour is unchanged.
 */
@RequiredArgsConstructor
public class MenuRankupCommand implements CommandExecutor {

  private final ArkonasRanksPlugin plugin;
  private final MenuModule menu;
  private final CommandExecutor parity;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // console keeps the exact parity behaviour (chat output, forced rankups)
    if (!(sender instanceof Player)) {
      return parity.onCommand(sender, command, label, args);
    }
    Player player = (Player) sender;

    // noconfirm bypasses the menu, just like Rankup3
    if (plugin.getConfig().getBoolean("enable-noconfirm", true)
        && args.length > 0 && args[0].equalsIgnoreCase("noconfirm")) {
      return parity.onCommand(sender, command, label, args);
    }

    if (plugin.error(player)) {
      return true;
    }

    // a named extra ladder bypasses the menu and uses the parity ladder routing
    if (args.length > 0 && plugin.getLadders() != null
        && !args[0].equalsIgnoreCase(com.arkonas.ranks.ladder.LadderRegistry.DEFAULT)
        && plugin.getLadders().has(args[0])) {
      return parity.onCommand(sender, command, label, args);
    }

    if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
      if (!player.hasPermission("rankup.top")) {
        return true;
      }
      boolean prestige = args.length > 1 && args[1].toLowerCase().startsWith("prestige");
      menu.openLeaderboard(player, prestige);
      return true;
    }

    // not in any ladder -> parity message (keeps the /ranks hint)
    RankElement<Rank> element = plugin.getRankups() == null ? null
        : plugin.getRankups().getByPlayer(player);
    if (element == null) {
      return parity.onCommand(sender, command, label, args);
    }

    menu.openRankup(player);
    return true;
  }
}
