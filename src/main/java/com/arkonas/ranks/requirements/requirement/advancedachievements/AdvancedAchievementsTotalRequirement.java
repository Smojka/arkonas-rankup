package com.arkonas.ranks.requirements.requirement.advancedachievements;

import com.hm.achievement.api.AdvancedAchievementsAPIFetcher;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;
import com.arkonas.ranks.requirements.ProgressiveRequirement;

public class AdvancedAchievementsTotalRequirement extends ProgressiveRequirement {
  public AdvancedAchievementsTotalRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "advancedachievements-total");
  }

  private AdvancedAchievementsTotalRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return AdvancedAchievementsAPIFetcher.fetchInstance().get().getPlayerTotalAchievements(player.getUniqueId());
  }

  @Override
  public Requirement clone() {
    return new AdvancedAchievementsTotalRequirement(this);
  }
}
