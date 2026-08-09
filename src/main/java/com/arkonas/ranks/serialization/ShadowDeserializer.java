package com.arkonas.ranks.serialization;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShadowDeserializer {

  public static List<RankSerialized> deserialize(UnmodifiableConfig ranks) {
    List<RankSerialized> ranksList = new ArrayList<>(ranks.size());
    for (Entry entry : ranks.entrySet()) {
      UnmodifiableConfig value = entry.getValue();
      if (value == null) continue;
      String rank = value.get("rank");
      String next = value.get("next");
      String displayName = value.get("display-name");
      List<String> commands = value.getOrElse("commands", Collections.emptyList());
      List<String> requirements;
      Map<String, List<String>> prestigeRequirements;
      Object requirementsObject = value.get("requirements");
      if (requirementsObject instanceof UnmodifiableConfig) {
        requirements = null;
        UnmodifiableConfig requirementsConfig = (UnmodifiableConfig) requirementsObject;
        prestigeRequirements = new HashMap<>(requirementsConfig.size());
        for (Entry requirementEntry : requirementsConfig.entrySet()) {
          prestigeRequirements.put(requirementEntry.getKey(), requirementEntry.getValue());
        }
      } else {
        prestigeRequirements = null;
        if (requirementsObject instanceof String) {
          requirements = Collections.singletonList((String) requirementsObject);
        } else if (requirementsObject instanceof List) {
          requirements = (List<String>) requirementsObject;
        } else {
          requirements = Collections.emptyList();
        }
      }

      UnmodifiableConfig messagesConfig = value.get("rankup");
      Map<String, String> messages;
      if (messagesConfig != null) {
        messages = new HashMap<>();
        updateMap(messages, messagesConfig, "rankup.");
      } else {
        messages = Collections.emptyMap();
      }

      Object costMultiplierObject = value.get("cost-multiplier");
      double costMultiplier = costMultiplierObject instanceof Number
          ? ((Number) costMultiplierObject).doubleValue() : 1.0;

      RankSerialized serialized = new RankSerialized(rank, next, displayName, commands, requirements, prestigeRequirements, messages, costMultiplier);

      // capture the hand-written menu lore keys from TOML too, matching the YAML path
      Map<String, List<String>> lore = readLore(value);
      if (lore != null) {
        serialized.setLore(lore);
      }

      // capture a per-rank celebration: override from TOML too, matching the YAML path, preserving
      // value types (booleans/numbers/lists) so EffectsListener parses them correctly
      Object celebrationObject = value.get("celebration");
      if (celebrationObject instanceof UnmodifiableConfig) {
        serialized.setCelebration(flattenTyped((UnmodifiableConfig) celebrationObject));
      }

      ranksList.add(serialized);
    }
    return ranksList;
  }

  /**
   * TOML twin of the YAML {@code readLore}: each {@code lore} / {@code lore-<variant>}
   * key accepts a single string or an array of lines.
   *
   * @return the populated keys, or null when the rank declares no lore at all
   */
  private static Map<String, List<String>> readLore(UnmodifiableConfig value) {
    Map<String, List<String>> lore = null;
    for (String key : RankSerialized.LORE_KEYS) {
      Object raw = value.get(key);
      List<String> lines;
      if (raw instanceof List) {
        lines = new ArrayList<>();
        for (Object line : (List<?>) raw) {
          if (line != null) {
            lines.add(String.valueOf(line));
          }
        }
      } else if (raw != null) {
        lines = Collections.singletonList(String.valueOf(raw));
      } else {
        continue;
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

  /** Flattens a TOML sub-config to dotted leaf keys with their original typed values. */
  private static Map<String, Object> flattenTyped(UnmodifiableConfig config) {
    Map<String, Object> map = new HashMap<>();
    collectTyped(map, config, "");
    return map;
  }

  private static void collectTyped(Map<String, Object> map, UnmodifiableConfig config, String prefix) {
    for (Entry entry : config.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof UnmodifiableConfig) {
        collectTyped(map, (UnmodifiableConfig) value, prefix + entry.getKey() + ".");
      } else if (value != null) {
        map.put(prefix + entry.getKey(), value);
      }
    }
  }

  private static void updateMap(Map<String, String> map, UnmodifiableConfig config, String prefix) {
    for (Entry message : config.entrySet()) {
      Object value = message.getValue();
      if (value != null) {
        if (value instanceof UnmodifiableConfig) {
          updateMap(map, (UnmodifiableConfig) value, prefix + message.getKey() + ".");
        } else {
          map.put(prefix + message.getKey(), String.valueOf(value));
        }
      }
    }
  }
}
