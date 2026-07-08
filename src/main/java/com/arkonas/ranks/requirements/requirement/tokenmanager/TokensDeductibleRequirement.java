package com.arkonas.ranks.requirements.requirement.tokenmanager;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.DeductibleRequirement;

public class TokensDeductibleRequirement extends TokensRequirement implements DeductibleRequirement {
  public TokensDeductibleRequirement(ArkonasRanksPlugin plugin, String name) {
    super(plugin, name);
  }

  protected TokensDeductibleRequirement(TokensDeductibleRequirement clone) {
    super(clone);
  }

  @Override
  public void apply(Player player, double multiplier) {
    manager.removeTokens(player, Math.round(getValueInt() * multiplier));
  }

  /** Deducts the discounted token cost so the amount taken matches {@link #getTotal(Player)}. */
  @Override
  public void apply(Player player) {
    apply(player, costFactor(player));
  }

  @Override
  public TokensRequirement clone() {
    return new TokensDeductibleRequirement(this);
  }
}
