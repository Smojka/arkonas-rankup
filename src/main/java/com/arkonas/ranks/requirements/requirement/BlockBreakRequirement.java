package com.arkonas.ranks.requirements.requirement;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class BlockBreakRequirement extends ProgressiveRequirement {
  public BlockBreakRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "block-break", true);
  }

  protected BlockBreakRequirement(BlockBreakRequirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    Material material = Material.matchMaterial(getSub());
    if (material == null || !material.isBlock()) {
      throw new IllegalArgumentException("'" + getSub() + "' is not a valid block");
    }
    return player.getStatistic(Statistic.MINE_BLOCK, material);
  }

  @Override
  public Requirement clone() {
    return new BlockBreakRequirement(this);
  }
}
