package com.arkonas.ranks.requirements.requirement.towny;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

public class TownyResidentRequirement extends Requirement {
  public TownyResidentRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "towny-resident");
  }

  protected TownyResidentRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    return TownyUtils.getInstance().isResident(player) == getValueBoolean();
  }

  @Override
  public Requirement clone() {
    return new TownyResidentRequirement(this);
  }
}
