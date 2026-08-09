package com.arkonas.ranks.ranks.requirements;

import java.util.List;
import org.bukkit.entity.Player;
import com.arkonas.ranks.requirements.DeductibleRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class ListRankRequirements implements RankRequirements {
  private final List<Requirement> requirements;

  public ListRankRequirements(List<Requirement> requirements) {
    this.requirements = requirements;
  }

  @Override
  public Iterable<Requirement> getRequirements(Player player) {
    return requirements;
  }

  @Override
  public boolean hasRequirements(Player player) {
    for (Requirement requirement : requirements) {
      if (!requirement.check(player)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Requirement getRequirement(Player player, String name) {
    for (Requirement requirement : requirements) {
      if (requirement.getFullName().equalsIgnoreCase(name)) {
        return requirement;
      }
    }
    return null;
  }

  @Override
  public void applyRequirements(Player player) {
    // Re-verify immediately before charging. Callers are supposed to have checked already, but the
    // check and the charge are separate calls with menu clicks, deferred tasks and other plugins'
    // listeners in between, and a deduction that runs on an unmet requirement is a free rank.
    // Throwing (rather than returning) aborts the caller before the group transfer, so a player can
    // never end up ranked up without having paid.
    if (!hasRequirements(player)) {
      throw new IllegalStateException(
          "Refusing to deduct rankup costs from " + player.getName()
              + ": they no longer meet the requirements.");
    }
    for (Requirement requirement : requirements) {
      if (requirement instanceof DeductibleRequirement) {
        ((DeductibleRequirement) requirement).apply(player);
      }
    }
  }

  @Override
  public void setPerRankFactor(double factor) {
    for (Requirement requirement : requirements) {
      requirement.setPerRankFactor(factor);
    }
  }
}
