package com.arkonas.ranks.commands;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.gui.Gui;
import com.arkonas.ranks.messages.Message;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.ranks.Rankups;

@RequiredArgsConstructor
public class RankupCommand implements CommandExecutor {
  // weak hash maps so players going offline are automatically removed.
  // otherwise there is a potential (albeit small) memory leak.
  private final Map<Player, Long> confirming = new WeakHashMap<>();
  private final ArkonasRanksPlugin plugin;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // handled before the error check so a config that failed to load can still be fixed and
    // reloaded in place, exactly like /aru reload does.
    if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
      handleReload(sender);
      return true;
    }

    if (plugin.error(sender)) {
      return true;
    }

    if (plugin.getConfig().getBoolean("enable-noconfirm", true) && args.length > 0 && args[0].equalsIgnoreCase("noconfirm")) {
      handleNoConfirm(sender, label, Arrays.copyOfRange(args, 1, args.length));
      return true;
    }

    if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
      handleTop(sender, Arrays.copyOfRange(args, 1, args.length));
      return true;
    }

    // check if player
    if (!(sender instanceof Player)) {
      return false;
    }
    Player player = (Player) sender;

    // /rankup <ladder> operates directly on a named extra ladder (requirements and
    // messages still apply). The menu/confirm flow stays on the default ladder.
    if (args.length > 0 && plugin.getLadders() != null
        && !args[0].equalsIgnoreCase(com.arkonas.ranks.ladder.LadderRegistry.DEFAULT)
        && plugin.getLadders().has(args[0])) {
      plugin.getHelper().rankup(player, plugin.getLadders().get(args[0]));
      return true;
    }

    Rankups rankups = plugin.getRankups();
    if (!plugin.getHelper().checkRankup(player)) {
      return true;
    }

    RankElement<Rank> rankElement = rankups.getByPlayer(player);

    FileConfiguration config = plugin.getConfig();
    String confirmationType = config.getString("confirmation-type").toLowerCase();

    // if they are on text confirming, rank them up
    // clicking on the gui cannot confirm a rankup
    if (confirmationType.equals("text") && confirming.containsKey(player) && !(args.length > 0 && args[0].equalsIgnoreCase("gui"))) {
      long time = System.currentTimeMillis() - confirming.remove(player);
      if (time < config.getInt("text.timeout") * 1000L) {
        plugin.getHelper().rankup(player);
        return true;
      }
    }

    switch (confirmationType) {
      case "text":
        confirming.put(player, System.currentTimeMillis());
        plugin.getMessage(rankElement.getRank(), Message.CONFIRMATION)
            .replacePlayer(player)
            .replaceOldRank(rankElement.getRank())
            .replaceRank(rankElement.getNext().getRank())
            .send(player);
        break;
      case "gui":
        Gui gui = Gui.of(player, rankElement.getRank(), rankElement.getNext().getRank(), plugin, args.length > 0 && args[0].equalsIgnoreCase("gui"));
        if (gui == null) {
          player.sendMessage(ChatColor.RED + "GUI is not available. Check console for more information.");
          return true;
        }
        gui.open(player);
        break;
      case "none":
        plugin.getHelper().rankup(player);
        break;
      default:
        throw new IllegalArgumentException("Invalid confirmation type " + confirmationType);
    }
    return true;
  }

  /**
   * Re-reads every configuration file (config.yml, the locale, rankups.yml, prestiges.yml,
   * ladders/, effects.yml and menus.yml) into memory. Same entry point as {@code /aru reload}.
   */
  private void handleReload(CommandSender sender) {
    if (!sender.hasPermission("rankup.reload")) {
      sender.sendMessage(ChatColor.RED + "You do not have permission to reload ArkonasRanks.");
      return;
    }

    plugin.reload(false);
    if (!plugin.error(sender)) {
      sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "ArkonasRanks "
          + ChatColor.YELLOW + "Reloaded configuration files.");
    }
  }

  private void handleTop(CommandSender sender, String[] args) {
    if (!sender.hasPermission("rankup.top")) {
      return;
    }
    com.arkonas.ranks.data.StatsService stats = plugin.getStats();
    if (stats == null) {
      sender.sendMessage(ChatColor.RED + "Statistics are disabled on this server.");
      return;
    }
    boolean prestiges = args.length > 0 && args[0].toLowerCase().startsWith("prestige");
    stats.top(prestiges, 10, entries -> Bukkit.getScheduler().runTask(plugin, () -> {
      sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD
          + (prestiges ? "Top Prestiges" : "Top Rankups"));
      if (entries.isEmpty()) {
        sender.sendMessage(ChatColor.GRAY + "No entries yet.");
        return;
      }
      int position = 1;
      for (com.arkonas.ranks.data.LeaderboardEntry entry : entries) {
        sender.sendMessage(ChatColor.YELLOW + "" + position++ + ". " + ChatColor.WHITE
            + entry.name() + ChatColor.GRAY + " - " + ChatColor.AQUA + entry.count());
      }
    }));
  }

  private void handleNoConfirm(CommandSender sender, String label, String[] args) {
    if (sender.hasPermission("rankup.noconfirm.other") && args.length > 0) {
      Player player = Bukkit.getPlayer(args[0]);
      if (player == null) {
        sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
      } else {
        plugin.getHelper().rankup(player);
        sender.sendMessage(ChatColor.GREEN + "Triggered no-confirmation rankup for " + player.getName());
      }
    } else {
      if (!(sender instanceof Player)) {
        sender.sendMessage("/" + label + " noconfirm <player>");
        return;
      }

      plugin.getHelper().rankup((Player) sender);
    }
  }
}
