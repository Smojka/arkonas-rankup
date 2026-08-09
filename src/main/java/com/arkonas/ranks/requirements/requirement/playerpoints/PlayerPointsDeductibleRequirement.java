package com.arkonas.ranks.requirements.requirement.playerpoints;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.DeductibleRequirement;
import com.arkonas.ranks.requirements.Requirement;

/** Deducts the PlayerPoints cost on rankup, honouring the cost multiplier. */
public class PlayerPointsDeductibleRequirement extends PlayerPointsRequirement
    implements DeductibleRequirement {

  public PlayerPointsDeductibleRequirement(ArkonasRanksPlugin plugin, String name) {
    super(plugin, name);
  }

  protected PlayerPointsDeductibleRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public void apply(Player player, double multiplier) {
    // getValueDouble (not getValueInt) so a non-integer configured cost never throws at deduction
    // time after the double-based affordability check already accepted it.
    // ceil, not round, so the deduction is never less than the cost that was checked
    access().take(player.getUniqueId(), (int) Math.ceil(getValueDouble() * multiplier - EPSILON));
  }

  /** Deducts the discounted points cost so the amount taken matches {@link #getTotal(Player)}. */
  @Override
  public void apply(Player player) {
    apply(player, costFactor(player));
  }

  @Override
  public Requirement clone() {
    return new PlayerPointsDeductibleRequirement(this);
  }
}
