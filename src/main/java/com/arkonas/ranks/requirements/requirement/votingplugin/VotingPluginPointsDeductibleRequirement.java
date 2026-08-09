package com.arkonas.ranks.requirements.requirement.votingplugin;

import com.bencodez.votingplugin.VotingPluginMain;
import com.bencodez.votingplugin.user.VotingPluginUser;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.DeductibleRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class VotingPluginPointsDeductibleRequirement extends VotingPluginPointsRequirement implements DeductibleRequirement {

  public VotingPluginPointsDeductibleRequirement(ArkonasRanksPlugin plugin, String name) {
    super(plugin, name);
  }

  protected VotingPluginPointsDeductibleRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public void apply(Player player, double multiplier) {
    // ceil, not round, so the deduction is never less than the cost the affordability check used
    int points = (int) Math.ceil(getValueDouble() * multiplier - EPSILON);
    if (points <= 0) {
      return;
    }
    VotingPluginUser user = VotingPluginMain.getPlugin().getVotingPluginUserManager().getVotingPluginUser(player);
    // throw rather than warn: the caller grants the rank as soon as this returns, so a refused
    // deduction that only logged would hand out a free rankup
    if (!user.removePoints(points)) {
      throw new IllegalStateException("VotingPlugin refused to remove " + points + " points from "
          + player.getName() + "; the rankup was cancelled.");
    }
  }

  /** Deducts the discounted points cost so the amount taken matches {@link #getTotal(Player)}. */
  @Override
  public void apply(Player player) {
    apply(player, costFactor(player));
  }

  @Override
  public Requirement clone() {
    return new VotingPluginPointsDeductibleRequirement(this);
  }
}
