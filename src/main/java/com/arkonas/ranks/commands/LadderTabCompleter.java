package com.arkonas.ranks.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.Nullable;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.ladder.LadderRegistry;

/**
 * Completes the first argument of {@code /rankup} and {@code /maxrankup} with the ids of the
 * configured extra ladders, plus any fixed sub-commands the command also accepts (e.g. noconfirm,
 * top). Only the first argument is handled; later arguments fall back to the server default.
 */
public class LadderTabCompleter implements TabCompleter {

  private final ArkonasRanksPlugin plugin;
  private final String[] extraFirstArgs;

  public LadderTabCompleter(ArkonasRanksPlugin plugin, String... extraFirstArgs) {
    this.plugin = plugin;
    this.extraFirstArgs = extraFirstArgs;
  }

  @Nullable
  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias,
      String[] args) {
    if (args.length != 1) {
      return null; // let Bukkit use the default (e.g. player names) for later args
    }
    String prefix = args[0].toLowerCase(Locale.ROOT);
    List<String> out = new ArrayList<>();
    for (String extra : extraFirstArgs) {
      if (extra.toLowerCase(Locale.ROOT).startsWith(prefix)) {
        out.add(extra);
      }
    }
    LadderRegistry ladders = plugin.getLadders();
    if (ladders != null) {
      for (String id : ladders.ids()) {
        if (!id.equals(LadderRegistry.DEFAULT) && id.startsWith(prefix)) {
          out.add(id);
        }
      }
    }
    return out;
  }
}
