package com.arkonas.ranks.ranks.requirements;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

public class RankRequirementsFactory {
  private static final String REQUIREMENTS = "requirements";

  public static RankRequirements getRequirements(ArkonasRanksPlugin plugin, ConfigurationSection section) {
    double perRankFactor = section.getDouble("cost-multiplier", 1.0);
    if (section.isConfigurationSection(REQUIREMENTS)) {
      return applyFactor(
          getPrestigeListRequirements(plugin, section.getConfigurationSection(REQUIREMENTS)),
          perRankFactor);
    } else {
      return applyFactor(
          getListRequirements(plugin, getRequirementStrings(section, REQUIREMENTS)), perRankFactor);
    }
  }

  /** Stamps a per-rank cost multiplier onto every requirement built for the rank. */
  private static RankRequirements applyFactor(RankRequirements requirements, double perRankFactor) {
    if (requirements != null && perRankFactor != 1.0) {
      requirements.setPerRankFactor(perRankFactor);
    }
    return requirements;
  }

  public static RankRequirements getRequirements(ArkonasRanksPlugin plugin, List<String> requirements,
      Map<String, List<String>> prestigeRequirements) {
    return getRequirements(plugin, requirements, prestigeRequirements, 1.0);
  }

  public static RankRequirements getRequirements(ArkonasRanksPlugin plugin, List<String> requirements,
      Map<String, List<String>> prestigeRequirements, double perRankFactor) {
    RankRequirements built;
    if (prestigeRequirements != null) {
      ConfigurationSection section = new MemoryConfiguration();
      for (Map.Entry<String, List<String>> entry : prestigeRequirements.entrySet()) {
        section.set(entry.getKey(), entry.getValue());
      }
      built = getPrestigeListRequirements(plugin, section);
    } else if (requirements != null) {
      built = getListRequirements(plugin, requirements);
    } else {
//      throw new IllegalArgumentException("No requirements set.");
      return null;
    }
    return applyFactor(built, perRankFactor);
  }

  private static Collection<String> getRequirementStrings(ConfigurationSection section, String key) {
    if (section.isList(key)) {
      return section.getStringList(key);
    } else {
      String string = section.getString(key);
      if (string == null) {
        return null;
      } else {
        return Collections.singleton(string);
      }
    }
  }

  private static List<Requirement> stringsToRequirements(ArkonasRanksPlugin plugin, Iterable<String> strings) {
    return plugin.getRequirements().getRequirements(strings);
  }

  private static RankRequirements getListRequirements(ArkonasRanksPlugin plugin, Iterable<String> list) {
    List<Requirement> requirements = stringsToRequirements(plugin, list);
    return new ListRankRequirements(requirements);
  }

  private static RankRequirements getPrestigeListRequirements(ArkonasRanksPlugin plugin, ConfigurationSection section) {
    if (plugin.getPrestiges() == null) {
      throw new IllegalArgumentException("Prestige requirements are being used but prestiging is not enabled.");
    }

    RankRequirements defaultRequirements = null;
    Map<String, RankRequirements> requirements = new HashMap<>();

    for (String key : section.getKeys(false)) {
      Collection<String> stringRequirements = getRequirementStrings(section, key);
      if (stringRequirements != null) {
        RankRequirements rankRequirements = getListRequirements(plugin, stringRequirements);
        if ("default".equalsIgnoreCase(key)) {
          defaultRequirements = rankRequirements;
        } else {
          requirements.put(key.toLowerCase(), rankRequirements);
        }
      }
    }

    if (defaultRequirements == null) {
      throw new IllegalArgumentException("No default requirements set for rank " + section.getParent().getName() + ". See the wiki for info.");
    }

    return new PrestigeListRankRequirements(plugin, defaultRequirements, requirements);
  }
}
