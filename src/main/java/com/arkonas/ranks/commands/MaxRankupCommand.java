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
  /**
   * Safety bound on a single pass, matching {@code AutoRankup}. Without it a rank cycle in the
   * config, or a setup where the rank never actually changes (see the no-progress guard below),
   * spins the main thread forever — a server freeze any player can trigger with one command.
   */
  private static final int MAX_ITERATIONS = 1000;

  private final ArkonasRanksPlugin plugin;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      return false;
    }
    if (plugin.error(sender)) {
      return true;
    }
    RankupHelper helper = plugin.getHelper();

    Player player = (Player) sender;

    // /maxrankup <ladder> maxes a named extra ladder; no arg = the default ladder
    Rankups target = plugin.getRankups();
    if (args.length > 0 && plugin.getLadders() != null && plugin.getLadders().has(args[0])) {
      target = plugin.getLadders().get(args[0]);
    }
    if (target == null) {
      return true;
    }

    if (!helper.checkRankup(player, target, true)) {
      return true;
    }

    int iterations = 0;
    do {
      RankElement<Rank> rank = target.getByPlayer(player);
      if (rank == null || !rank.hasNext()) {
        break;
      }
      if (!helper.applyCost(player, rank.getRank())) {
        break; // a refused deduction must not grant the rank
      }

      helper.doRankup(player, rank);

      // if the individual-messages setting is disabled, only send the "well done you ranked up"
      // messages if they can't rank up any more.
      if (plugin.getConfig().getBoolean("max-rankup.individual-messages")
          || !helper.checkRankup(player, target, false)) {
        helper.sendRankupMessages(player, rank);
      }

      // no-progress guard: if the player is still on the same rank after doRankup the group change
      // did not take (permission-rankup mode makes transferGroup a no-op, and a permission backend
      // can fail silently). Continuing would charge them the same rank's cost on every pass.
      RankElement<Rank> after = target.getByPlayer(player);
      if (after == rank) {
        plugin.getLogger().warning("Stopping /maxrankup for " + player.getName()
            + ": they are still on rank '" + rank.getRank().getRank() + "' after ranking up."
            + " Check that the permission plugin is applying group changes"
            + " (permission-rankup mode cannot move players between ranks).");
        break;
      }

      if (++iterations >= MAX_ITERATIONS) {
        plugin.getLogger().warning("Stopping /maxrankup for " + player.getName() + " after "
            + MAX_ITERATIONS + " rankups; check rankups.yml for a rank loop.");
        break;
      }
    } while (helper.checkRankup(player, target, false));

    return true;
  }
}
