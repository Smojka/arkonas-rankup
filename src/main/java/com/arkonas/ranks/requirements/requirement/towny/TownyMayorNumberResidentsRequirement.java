package com.arkonas.ranks.requirements.requirement.towny;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class TownyMayorNumberResidentsRequirement extends ProgressiveRequirement {
  public TownyMayorNumberResidentsRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "towny-mayor-residents");
  }

  protected TownyMayorNumberResidentsRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    if (TownyUtils.getInstance().isMayor(player)) {
      return TownyUtils.getInstance().getTown(player).getNumResidents();
    } else {
      return 0;
    }
  }

  @Override
  public Requirement clone() {
    return new TownyMayorNumberResidentsRequirement(this);
  }
}
