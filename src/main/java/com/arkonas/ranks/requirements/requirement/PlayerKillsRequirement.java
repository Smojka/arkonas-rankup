package com.arkonas.ranks.requirements.requirement;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class PlayerKillsRequirement extends ProgressiveRequirement {
  public PlayerKillsRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "player-kills");
  }

  protected PlayerKillsRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return player.getStatistic(Statistic.PLAYER_KILLS);
  }

  @Override
  public Requirement clone() {
    return new PlayerKillsRequirement(this);
  }
}
