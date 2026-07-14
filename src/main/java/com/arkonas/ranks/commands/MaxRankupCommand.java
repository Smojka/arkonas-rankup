package com.arkonas.ranks.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.RankupHelper;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.ranks.Rankups;

@RequiredArgsConstructor
public class MaxRankupCommand implements CommandExecutor {
  private final ArkonasRanksPlugin plugin;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      return false;
    }
    RankupHelper helper = plugin.getHelper();

    Player player = (Player) sender;

    // /maxrankup <ladder> maxes a named extra ladder; no arg = the default ladder
    Rankups target = plugin.getRankups();
    if (args.length > 0 && plugin.getLadders() != null && plugin.getLadders().has(args[0])) {
      target = plugin.getLadders().get(args[0]);
    }

    if (!helper.checkRankup(player, target, true)) {
      return true;
    }

    do {
      RankElement<Rank> rank = target.getByPlayer(player);
      rank.getRank().applyRequirements(player);

      helper.doRankup(player, rank);

      // if the individual-messages setting is disabled, only send the "well done you ranked up"
      // messages if they can't rank up any more.
      if (plugin.getConfig().getBoolean("max-rankup.individual-messages")
          || !helper.checkRankup(player, target, false)) {
        helper.sendRankupMessages(player, rank);
      }
    } while (helper.checkRankup(player, target, false));

    return true;
  }
}
