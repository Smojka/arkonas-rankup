package com.arkonas.ranks.requirements.requirement.towny;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class TownyKingNumberResidentsRequirement extends ProgressiveRequirement {
  public TownyKingNumberResidentsRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "towny-king-residents");
  }

  protected TownyKingNumberResidentsRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    if (TownyUtils.getInstance().isKing(player)) {
      return TownyUtils.getInstance().getNation(player).getNumResidents();
    } else {
      return 0;
    }
  }

  @Override
  public Requirement clone() {
    return new TownyKingNumberResidentsRequirement(this);
  }
}
