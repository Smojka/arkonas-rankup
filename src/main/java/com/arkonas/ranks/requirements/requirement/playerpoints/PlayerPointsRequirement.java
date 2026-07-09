package com.arkonas.ranks.requirements.requirement.playerpoints;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

/**
 * A rank cost paid in PlayerPoints ({@code playerpoints <n>} / {@code playerpointsh <n>}), separate
 * from using PlayerPoints as the money backend. Scales by the cost multiplier like the other
 * currencies; the balance is read through the reflection {@link PlayerPointsAccess}.
 */
public class PlayerPointsRequirement extends ProgressiveRequirement {

  private static volatile PlayerPointsAccess access;

  public PlayerPointsRequirement(ArkonasRanksPlugin plugin, String name) {
    super(plugin, name);
  }

  protected PlayerPointsRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return access().look(player.getUniqueId());
  }

  /** Effective points cost after any active cost multiplier, so a sale discounts points too. */
  @Override
  public double getTotal(Player player) {
    return getValueDouble() * costFactor(player);
  }

  static PlayerPointsAccess access() {
    PlayerPointsAccess local = access;
    if (local == null) {
      local = PlayerPointsAccess.create();
      access = local;
    }
    return local;
  }

  @Override
  public Requirement clone() {
    return new PlayerPointsRequirement(this);
  }
}
