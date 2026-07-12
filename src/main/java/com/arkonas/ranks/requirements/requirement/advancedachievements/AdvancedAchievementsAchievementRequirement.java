package com.arkonas.ranks.requirements.requirement.advancedachievements;

import com.hm.achievement.api.AdvancedAchievementsAPI;
import com.hm.achievement.api.AdvancedAchievementsAPIFetcher;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

public class AdvancedAchievementsAchievementRequirement extends Requirement {
  public AdvancedAchievementsAchievementRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "advancedachievements-achievement");
  }

  protected AdvancedAchievementsAchievementRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    try {
      AdvancedAchievementsAPI api = AdvancedAchievementsAPIFetcher.fetchInstance().get();
      return api.hasPlayerReceivedAchievement(player.getUniqueId(), getValueString());
    } catch (Throwable t) {
      // AdvancedAchievements not ready / API changed -> fail closed (requirement unmet)
      logHookFailureOnce(t);
      return false;
    }
  }

  @Override
  public String getFullName() {
    return super.getFullName() + "#" + getValueString();
  }

  @Override
  public double getTotal(Player player) {
    return 1;
  }

  @Override
  public Requirement clone() {
    return new AdvancedAchievementsAchievementRequirement(this);
  }
}
