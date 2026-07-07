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
    VotingPluginUser user = VotingPluginMain.getPlugin().getVotingPluginUserManager().getVotingPluginUser(player);
    if(!user.removePoints(getValueInt()))  {
      plugin.getLogger().warning("Unable to remove VotingPlugin points");
    }
  }

  @Override
  public Requirement clone() {
    return new VotingPluginPointsDeductibleRequirement(this);
  }
}
