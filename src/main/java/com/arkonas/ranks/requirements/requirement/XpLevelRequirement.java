package com.arkonas.ranks.requirements.requirement;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

public class XpLevelRequirement extends ProgressiveRequirement {
  public XpLevelRequirement(ArkonasRanksPlugin plugin, String name) {
    super(plugin, name);
  }

  protected XpLevelRequirement(XpLevelRequirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return player.getLevel();
  }

  @Override
  public Requirement clone() {
    return new XpLevelRequirement(this);
  }
}