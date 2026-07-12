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
  private final ArkonasRanksPlugin plugin;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      return false;
    }
    if (plugin.getPrestiges() == null) {
      return true; // prestige disabled; the command is a no-op, matching the /prestige gate
    }
    RankupHelper helper = plugin.getHelper();
    Player player = (Player) sender;

    if (!helper.checkPrestige(player, true)) {
      return true;
    }

    do {
      RankElement<Prestige> prestige = plugin.getPrestiges().getByPlayer(player);
      prestige.getRank().applyRequirements(player);

      helper.doPrestige(player, prestige);

      if (plugin.getConfig().getBoolean("max-rankup.individual-messages")
          || !helper.checkPrestige(player, false)) {
        helper.sendPrestigeMessages(player, prestige);
      }
    } while (helper.checkPrestige(player, false));

    return true;
  }
}
