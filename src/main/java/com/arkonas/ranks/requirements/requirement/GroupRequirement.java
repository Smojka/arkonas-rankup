package com.arkonas.ranks.requirements.requirement;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

public class GroupRequirement extends Requirement {
  public GroupRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "group");
  }

  protected GroupRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    for (String group : getValuesString()) {
      if (plugin.getPermissions().inGroup(player.getUniqueId(), group)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Requirement clone() {
    return new GroupRequirement(this);
  }
}
