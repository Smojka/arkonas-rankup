package com.arkonas.ranks.requirements.requirement;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class MoneyRequirement extends ProgressiveRequirement {
  public MoneyRequirement(ArkonasRanksPlugin plugin, String name) {
    super(plugin, name);
  }

  protected MoneyRequirement(MoneyRequirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return plugin.getEconomy().getBalance(player);
  }

  @Override
  public Requirement clone() {
    return new MoneyRequirement(this);
  }
}