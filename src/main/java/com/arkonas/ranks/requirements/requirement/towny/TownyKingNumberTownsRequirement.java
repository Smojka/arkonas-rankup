package com.arkonas.ranks.requirements.requirement.towny;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class TownyKingNumberTownsRequirement extends ProgressiveRequirement {
  public TownyKingNumberTownsRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "towny-king-towns");
  }

  protected TownyKingNumberTownsRequirement(TownyKingNumberTownsRequirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    if (TownyUtils.getInstance().isKing(player)) {
      return TownyUtils.getInstance().getNation(player).getNumTowns();
    } else {
      return 0;
    }
  }

  @Override
  public Requirement clone() {
    return new TownyKingNumberTownsRequirement(this);
  }
}
