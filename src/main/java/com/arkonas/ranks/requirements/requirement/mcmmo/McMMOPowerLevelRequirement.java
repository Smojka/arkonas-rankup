package com.arkonas.ranks.requirements.requirement.mcmmo;

import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;
import com.arkonas.ranks.requirements.ProgressiveRequirement;

public class McMMOPowerLevelRequirement extends ProgressiveRequirement {
  public McMMOPowerLevelRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "mcmmo-power-level");
  }

  protected McMMOPowerLevelRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return UserManager.getPlayer(player).getPowerLevel();
  }

  @Override
  public Requirement clone() {
    return new McMMOPowerLevelRequirement(this);
  }
}
