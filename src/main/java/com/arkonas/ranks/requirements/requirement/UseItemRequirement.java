package com.arkonas.ranks.requirements.requirement;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class UseItemRequirement extends ProgressiveRequirement {
  public UseItemRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "use-item", true);
  }

  protected UseItemRequirement(UseItemRequirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    Material material = Material.matchMaterial(getSub());
    if (material == null) {
      throw new IllegalArgumentException("'" + getSub() + "' is not a valid item");
    }
    return player.getStatistic(Statistic.USE_ITEM, material);
  }

  @Override
  public Requirement clone() {
    return new UseItemRequirement(this);
  }
}
