package com.arkonas.ranks.serialization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;

public class YamlDeserializer {

  public static List<RankSerialized> deserialize(ConfigurationSection ranks) {
    Set<String> rankKeys = ranks.getKeys(false);
    List<RankSerialized> ranksList = new ArrayList<>(rankKeys.size());
    for (String rankKey : rankKeys) {
      ConfigurationSection section = ranks.getConfigurationSection(rankKey);
      if (section == null) continue;
      String rank = section.getString("rank");
      String next = section.getString("next");
      String displayName = section.getString("display-name");
      List<String> commands = section.getStringList("commands");
      List<String> requirements;
      Map<String, List<String>> prestigeRequirements;
      if (section.isConfigurationSection("requirements")) {
        requirements = null;
        ConfigurationSection requirementsSection = section.getConfigurationSection("requirements");
        Set<String> keys = requirementsSection.getKeys(false);
        prestigeRequirements = new HashMap<>(keys.size());
        for (String key : keys) {
          prestigeRequirements.put(key, requirementsSection.getStringList(key));
        }
      } else {
        prestigeRequirements = null;
        requirements = section.getStringList("requirements");
      }

      ConfigurationSection rankupSection = section.getConfigurationSection("rankup");
      Map<String, String> messages;
      if (rankupSection != null) {

        Set<String> rankup = rankupSection.getKeys(true);
        messages = new HashMap<>(rankup.size());
        for (String key : rankup) {
          if (!rankupSection.isConfigurationSection(key)) {
            messages.put(MemorySection.createPath(rankupSection, key, section), rankupSection.getString(key));
          }
        }
      } else {
        messages = Collections.emptyMap();
      }

      double costMultiplier = section.getDouble("cost-multiplier", 1.0);

      RankSerialized serialized = new RankSerialized(rank, next, displayName, commands, requirements, prestigeRequirements, messages, costMultiplier);

      // capture the hand-written menu lore keys so RankLore can read them off the rank's section
      Map<String, List<String>> lore = readLore(section);
      if (lore != null) {
        serialized.setLore(lore);
      }

      // capture a per-rank celebration: override, preserving value types (booleans, numbers, lists)
      ConfigurationSection celebrationSection = section.getConfigurationSection("celebration");
      if (celebrationSection != null) {
        Map<String, Object> celebration = new HashMap<>();
        for (String key : celebrationSection.getKeys(true)) {
          if (!celebrationSection.isConfigurationSection(key)) {
            celebration.put(key, celebrationSection.get(key));
          }
        }
        serialized.setCelebration(celebration);
      }

      ranksList.add(serialized);
    }
    return ranksList;
  }

  /**
   * Reads the {@code lore} / {@code lore-<variant>} keys off a rank section. Each
   * accepts either a single string (split on {@code \n} later) or a list of lines;
   * absent and empty keys are skipped so the next fallback applies.
   *
   * @return the populated keys, or null when the rank declares no lore at all
   */
  private static Map<String, List<String>> readLore(ConfigurationSection section) {
    Map<String, List<String>> lore = null;
    for (String key : RankSerialized.LORE_KEYS) {
      List<String> lines;
      if (section.isList(key)) {
        lines = section.getStringList(key);
      } else {
        String single = section.getString(key);
        lines = single == null ? Collections.emptyList() : Collections.singletonList(single);
      }
      if (lines.isEmpty()) {
        continue;
      }
      if (lore == null) {
        lore = new HashMap<>();
      }
      lore.put(key, lines);
    }
    return lore;
  }

}
