package com.arkonas.ranks.ranks.requirements;

import java.util.Map;
import java.util.Objects;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.prestige.Prestige;
import com.arkonas.ranks.prestige.Prestiges;
import com.arkonas.ranks.requirements.Requirement;

public class PrestigeListRankRequirements implements RankRequirements {
  private final ArkonasRanksPlugin plugin;
  private final RankRequirements defaultRequirements;
  private final Map<String, RankRequirements> requirements;

  public PrestigeListRankRequirements(ArkonasRanksPlugin plugin, RankRequirements defaultRequirements, Map<String, RankRequirements> requirements) {
    Objects.requireNonNull(plugin);
    Objects.requireNonNull(defaultRequirements);

    this.plugin = plugin;
    this.defaultRequirements = defaultRequirements;
    this.requirements = requirements;
  }

  @Override
  public Iterable<Requirement> getRequirements(Player player) {
    return getRankRequirements(player).getRequirements(player);
  }

  @Override
  public boolean hasRequirements(Player player) {
    return getRankRequirements(player).hasRequirements(player);
  }

  @Override
  public Requirement getRequirement(Player player, String name) {
    return getRankRequirements(player).getRequirement(player, name);
  }

  @Override
  public void applyRequirements(Player player) {
    getRankRequirements(player).applyRequirements(player);
  }

  private RankRequirements getRankRequirements(Player player) {
    Prestiges prestiges = plugin.getPrestiges();
    if (player == null || prestiges == null) {
      return defaultRequirements;
    }

    for (Prestige prestige : prestiges.getTree()) {
      String next = prestige.getNext();
      if(next != null && plugin.getPermissions().inGroup(player.getUniqueId(), next)) {
        RankRequirements rankRequirements = this.requirements.get(next.toLowerCase());
        if (rankRequirements != null) {
          return rankRequirements;
        }
      }
    }
    return defaultRequirements;
  }
}
