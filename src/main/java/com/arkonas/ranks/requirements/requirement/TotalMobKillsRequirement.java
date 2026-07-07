package com.arkonas.ranks.requirements.requirement;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class TotalMobKillsRequirement extends ProgressiveRequirement {
  public TotalMobKillsRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "total-mob-kills");
  }

  private TotalMobKillsRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return player.getStatistic(Statistic.MOB_KILLS);
  }

  @Override
  public Requirement clone() {
    return new TotalMobKillsRequirement(this);
  }
}
