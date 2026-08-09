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
    // ceil, not round, so the deduction is never less than the cost the affordability check used
    long tokens = (long) Math.ceil(getValueDouble() * multiplier - EPSILON);
    if (tokens <= 0) {
      return;
    }
    manager.removeTokens(player, tokens);
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
