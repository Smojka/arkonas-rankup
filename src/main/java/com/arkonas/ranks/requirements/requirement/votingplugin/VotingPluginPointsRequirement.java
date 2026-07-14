package com.arkonas.ranks.requirements.requirement.votingplugin;

import com.bencodez.votingplugin.VotingPluginMain;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class VotingPluginPointsRequirement extends ProgressiveRequirement {

  public VotingPluginPointsRequirement(ArkonasRanksPlugin plugin, String name) {
    super(plugin, name);
  }

  protected VotingPluginPointsRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return VotingPluginMain.getPlugin().getVotingPluginUserManager().getVotingPluginUser(player).getPoints();
  }

  /** Effective points cost after any active cost multiplier, so a sale discounts vote points too. */
  @Override
  public double getTotal(Player player) {
    return getValueDouble() * costFactor(player);
  }

  @Override
  public Requirement clone() {
    return new VotingPluginPointsRequirement(this);
  }
}
