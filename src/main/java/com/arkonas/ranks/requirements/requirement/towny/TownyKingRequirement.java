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
    try {
      return TownyUtils.getInstance().isKing(player) == getValueBoolean();
    } catch (Throwable t) {
      logHookFailureOnce(t);
      return false;
    }
  }

  @Override
  public Requirement clone() {
    return new TownyKingRequirement(this);
  }
}
