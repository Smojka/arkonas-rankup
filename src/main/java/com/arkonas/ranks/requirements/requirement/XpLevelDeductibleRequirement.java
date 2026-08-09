package com.arkonas.ranks.requirements.requirement;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.DeductibleRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class XpLevelDeductibleRequirement extends XpLevelRequirement implements DeductibleRequirement {

  public XpLevelDeductibleRequirement(ArkonasRanksPlugin plugin, String name) {
    super(plugin, name);
  }

  private XpLevelDeductibleRequirement(XpLevelDeductibleRequirement clone) {
    super(clone);
  }

  @Override
  public void apply(Player player, double multiplier) {
    // ceil, not round: the check compares the player's level against the exact double cost, so
    // rounding the deduction down would take fewer levels than they were checked for
    int levels = (int) Math.ceil(getValueDouble() * multiplier - EPSILON);
    if (levels <= 0) {
      return;
    }
    player.setLevel(Math.max(0, player.getLevel() - levels));
  }

  /** Deducts the discounted level cost so the amount taken matches {@link #getTotal(Player)}. */
  @Override
  public void apply(Player player) {
    apply(player, costFactor(player));
  }

  @Override
  public Requirement clone() {
    return new XpLevelDeductibleRequirement(this);
  }
}
