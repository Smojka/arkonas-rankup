package com.arkonas.ranks.requirements.requirement.superbvote;

import io.minimum.minecraft.superbvote.SuperbVote;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class SuperbVoteVotesRequirement extends ProgressiveRequirement {
  public SuperbVoteVotesRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "superbvote-votes");
  }

  private SuperbVoteVotesRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return SuperbVote.getPlugin().getVoteStorage().getVotes(player.getUniqueId()).getVotes();
  }

  @Override
  public Requirement clone() {
    return new SuperbVoteVotesRequirement(this);
  }
}
