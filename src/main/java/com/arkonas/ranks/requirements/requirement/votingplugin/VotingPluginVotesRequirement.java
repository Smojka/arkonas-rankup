package com.arkonas.ranks.requirements.requirement.votingplugin;

import com.bencodez.votingplugin.VotingPluginMain;
import com.bencodez.votingplugin.topvoter.TopVoter;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class VotingPluginVotesRequirement extends ProgressiveRequirement {
  public VotingPluginVotesRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "votingplugin-votes");
  }

  protected VotingPluginVotesRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return VotingPluginMain.getPlugin().getVotingPluginUserManager().getVotingPluginUser(player).getTotal(TopVoter.AllTime);
  }

  @Override
  public Requirement clone() {
    return new VotingPluginVotesRequirement(this);
  }
}
