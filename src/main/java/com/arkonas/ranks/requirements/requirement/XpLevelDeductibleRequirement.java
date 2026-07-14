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
    player.setLevel(player.getLevel() - (int) Math.round(getValueDouble() * multiplier));
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
