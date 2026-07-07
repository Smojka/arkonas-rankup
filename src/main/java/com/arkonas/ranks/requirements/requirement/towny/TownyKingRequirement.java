package com.arkonas.ranks.requirements.requirement.towny;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

public class TownyKingRequirement extends Requirement {
  public TownyKingRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "towny-king");
  }

  protected TownyKingRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    return TownyUtils.getInstance().isKing(player) == getValueBoolean();
  }

  @Override
  public Requirement clone() {
    return new TownyKingRequirement(this);
  }
}
