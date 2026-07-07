package com.arkonas.ranks.requirements.requirement;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

public class PermissionRequirement extends Requirement {
  public PermissionRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "permission");
  }

  protected PermissionRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    for (String permission : getValuesString()) {
      if (player.hasPermission(permission)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Requirement clone() {
    return new PermissionRequirement(this);
  }
}
