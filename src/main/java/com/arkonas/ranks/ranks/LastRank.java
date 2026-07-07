package com.arkonas.ranks.ranks;

import java.util.Collections;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.ranks.requirements.LastRankRequirements;

public class LastRank extends Rank {
  public LastRank(ArkonasRanksPlugin plugin, String name, String displayName) {
    super(null, plugin, null, name, displayName, new LastRankRequirements(), Collections.emptyList());
  }

  @Override
  public boolean hasRequirements(Player player) {
    return false;
  }

  @Override
  public void applyRequirements(Player player) {
  }

  @Override
  public void runCommands(Player player, Rank next) {
  }
}
