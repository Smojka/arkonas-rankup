package com.arkonas.ranks.ranks.requirements;

import org.bukkit.entity.Player;
import com.arkonas.ranks.requirements.Requirement;

public interface RankRequirements {
  Iterable<Requirement> getRequirements(Player player);

  boolean hasRequirements(Player player);
  Requirement getRequirement(Player player, String name);
  void applyRequirements(Player player);

  /** Applies a per-rank cost multiplier to every currency requirement held. No-op by default. */
  default void setPerRankFactor(double factor) {
  }
}
