package com.arkonas.ranks.requirements.requirement;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

public class WorldRequirement extends Requirement {
  public WorldRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "world");
  }

  protected WorldRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    String[] worlds = getValuesString();
    for (String world : worlds) {
      // a space-separated `world a b c` matches if the player is in ANY listed world; the previous
      // unconditional return only ever checked the first (mirrors Permission/Group semantics)
      if (player.getWorld().getName().equalsIgnoreCase(world)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public double getTotal(Player player) {
    return 1;
  }

  @Override
  public Requirement clone() {
    return new WorldRequirement(this);
  }
}
