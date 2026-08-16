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
 * the wrapped chat executor so their behaviour is unchanged.
 *
 * <p>Typed bare under one of the command's aliases — {@code /rank}, {@code /rütbe},
 * {@code /rutbe} — it opens the hub instead. Those labels are the noun, so they mean
 * "show me my ranks"; the command's own name is the verb and goes straight to the
 * rankup screen for a player who only wants to advance. Every sub-command
 * ({@code reload}, {@code top}, {@code noconfirm}, a named ladder) behaves the same
 * whichever label it was typed under.
 */
@RequiredArgsConstructor
public class MenuRankupCommand implements CommandExecutor {

  private final ArkonasRanksPlugin plugin;
  private final MenuModule menu;
  private final CommandExecutor chat;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // the admin reload sub-command is never a menu screen; the chat executor owns it (and it must
    // run before plugin.error(), so it stays usable when the config failed to load)
    if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
      return chat.onCommand(sender, command, label, args);
    }

    // console keeps the exact chat behaviour (chat output, forced rankups)
    if (!(sender instanceof Player)) {
      return chat.onCommand(sender, command, label, args);
    }
    Player player = (Player) sender;

    // noconfirm bypasses the menu
    if (plugin.getConfig().getBoolean("enable-noconfirm", true)
        && args.length > 0 && args[0].equalsIgnoreCase("noconfirm")) {
      return chat.onCommand(sender, command, label, args);
    }

    if (plugin.error(player)) {
      return true;
    }

    // bare /rank, /rütbe, /rutbe -> the hub. This is deliberately keyed on "not the command's own
    // name" rather than a copy of the alias list, so plugin.yml stays the one place they live.
    if (args.length == 0 && isAlias(command, label)) {
      menu.openHub(player);
      return true;
    }

    // a named extra ladder bypasses the menu and uses the chat ladder routing
    if (args.length > 0 && plugin.getLadders() != null
        && !args[0].equalsIgnoreCase(com.arkonas.ranks.ladder.LadderRegistry.DEFAULT)
        && plugin.getLadders().has(args[0])) {
      return chat.onCommand(sender, command, label, args);
    }

    if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
      if (!player.hasPermission("rankup.top")) {
        return true;
      }
      boolean prestige = args.length > 1 && args[1].toLowerCase().startsWith("prestige");
      menu.openLeaderboard(player, prestige);
      return true;
    }

    // not in any ladder -> chat message (keeps the /ranks hint)
    RankElement<Rank> element = plugin.getRankups() == null ? null
        : plugin.getRankups().getByPlayer(player);
    if (element == null) {
      return chat.onCommand(sender, command, label, args);
    }

    menu.openRankup(player);
    return true;
  }

  /**
   * Whether the command was typed under one of its aliases rather than its own name. The label
   * carries a {@code plugin:} prefix when the namespaced form was used, so only the part after
   * the last colon is compared.
   */
  private static boolean isAlias(Command command, String label) {
    if (label == null) {
      return false;
    }
    String typed = label.substring(label.lastIndexOf(':') + 1);
    return !typed.equalsIgnoreCase(command.getName());
  }
}
