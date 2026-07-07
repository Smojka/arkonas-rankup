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

  @Override
  public Requirement clone() {
    return new MoneyDeductibleRequirement(this);
  }
}
