package com.arkonas.ranks.requirements.requirement;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.economy.Economy;
import com.arkonas.ranks.requirements.DeductibleRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class MoneyDeductibleRequirement extends MoneyRequirement implements DeductibleRequirement {

  public MoneyDeductibleRequirement(ArkonasRanksPlugin plugin, String name) {
    super(plugin, name);
  }

  protected MoneyDeductibleRequirement(MoneyDeductibleRequirement clone) {
    super(clone);
  }

  @Override
  public void apply(Player player, double multiplier) {
    Economy economy = plugin.getEconomy();
    economy.withdrawPlayer(player, getValueDouble() * multiplier);
  }

  /**
   * Deducts the effective cost, applying the player's cost multiplier so the amount withdrawn
   * matches {@link #getTotal(Player)} shown in menus and placeholders.
   */
  @Override
  public void apply(Player player) {
    apply(player, costFactor(player));
  }

  @Override
  public Requirement clone() {
    return new MoneyDeductibleRequirement(this);
  }
}
