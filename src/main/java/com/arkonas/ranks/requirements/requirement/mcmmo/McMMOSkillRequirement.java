package com.arkonas.ranks.requirements.requirement.mcmmo;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;
import com.arkonas.ranks.requirements.ProgressiveRequirement;

public class McMMOSkillRequirement extends ProgressiveRequirement {
  public McMMOSkillRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "mcmmo", true);
  }

  protected McMMOSkillRequirement(McMMOSkillRequirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return McMMOSkillUtil.getInstance().getSkillLevel(player, getSub());
  }

  @Override
  public Requirement clone() {
    return new McMMOSkillRequirement(this);
  }
}
