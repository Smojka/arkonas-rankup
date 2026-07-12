package com.arkonas.ranks.serialization;

import java.util.List;
import java.util.Map;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

@Data
public class RankSerialized {

  private final String rank;
  private final String next;

  private final String displayName;

  private final List<String> commands;

  private final List<String> requirements;
  private final Map<String, List<String>> prestigeRequirements;

  private final Map<String, String> messages;

  /** Per-rank cost multiplier from the {@code cost-multiplier} config key; 1.0 leaves costs as-is. */
  private final double costMultiplier;

  /**
   * Flattened per-rank {@code celebration:} override (dotted leaf keys to their typed values), or
   * null when the rank has none. Non-final so the deserializers can attach it without threading it
   * through every {@code RankSerialized} construction site.
   */
  private Map<String, Object> celebration;

  public ConfigurationSection getMessagesAsSection() {
    ConfigurationSection section = new MemoryConfiguration();
    for (Map.Entry<String, String> entry : messages.entrySet()) {
      section.set(entry.getKey(), entry.getValue());
    }
    // re-attach the per-rank celebration override so EffectsListener.section() can find it; the
    // rankup path builds the rank's section here, and previously dropped this key entirely.
    if (celebration != null) {
      for (Map.Entry<String, Object> entry : celebration.entrySet()) {
        section.set("celebration." + entry.getKey(), entry.getValue());
      }
    }
    return section;
  }
}
