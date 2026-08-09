package com.arkonas.ranks.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.RankupHelper;
import com.arkonas.ranks.prestige.Prestige;
import com.arkonas.ranks.ranks.RankElement;

/**
 * {@code /maxprestige} — prestige as many times as possible in a single pass, the companion to
 * {@code /maxrankup}. In most setups a prestige resets the player to the first rank, so this
 * advances once; where a config lets a player meet several prestige tiers at once, it chains them.
 * Like {@code /maxrankup} it does not apply the per-step manual cooldown between chained prestiges.
 */
@RequiredArgsConstructor
public class MaxPrestigeCommand implements CommandExecutor {
  /** Safety bound on a single pass, matching {@code /maxrankup} and {@code AutoRankup}. */
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
    if (plugin.getPrestiges() == null) {
      return true; // prestige disabled; the command is a no-op, matching the /prestige gate
    }
    RankupHelper helper = plugin.getHelper();
    Player player = (Player) sender;

    if (!helper.checkPrestige(player, true)) {
      return true;
    }

    int iterations = 0;
    do {
      RankElement<Prestige> prestige = plugin.getPrestiges().getByPlayer(player);
      if (prestige == null || !prestige.hasNext()) {
        break;
      }
      if (!helper.applyCost(player, prestige.getRank())) {
        break; // a refused deduction must not grant the prestige
      }

      helper.doPrestige(player, prestige);

      if (plugin.getConfig().getBoolean("max-rankup.individual-messages")
          || !helper.checkPrestige(player, false)) {
        helper.sendPrestigeMessages(player, prestige);
      }

      // no-progress guard: if the group change did not take, the player is still on the same
      // prestige and another pass would charge them for it again
      RankElement<Prestige> after = plugin.getPrestiges().getByPlayer(player);
      if (after == prestige) {
        plugin.getLogger().warning("Stopping /maxprestige for " + player.getName()
            + ": they are still on the same prestige after prestiging."
            + " Check that the permission plugin is applying group changes.");
        break;
      }

      if (++iterations >= MAX_ITERATIONS) {
        plugin.getLogger().warning("Stopping /maxprestige for " + player.getName() + " after "
            + MAX_ITERATIONS + " prestiges; check prestiges.yml for a loop.");
        break;
      }
    } while (helper.checkPrestige(player, false));

    return true;
  }
}
