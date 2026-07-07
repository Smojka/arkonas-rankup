package com.arkonas.ranks.requirements.requirement.towny;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

public class TownyMayorRequirement extends Requirement {
  public TownyMayorRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "towny-mayor");
  }

  protected TownyMayorRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    return TownyUtils.getInstance().isMayor(player) == getValueBoolean();
  }

  @Override
  public Requirement clone() {
    return new TownyMayorRequirement(this);
  }
}
