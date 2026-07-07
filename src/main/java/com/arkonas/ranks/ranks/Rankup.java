package com.arkonas.ranks.ranks;

import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.ranks.requirements.RankRequirements;
import com.arkonas.ranks.ranks.requirements.RankRequirementsFactory;

import java.util.List;
import com.arkonas.ranks.serialization.RankSerialized;

public class Rankup extends Rank {
  public static Rankup deserialize(ArkonasRanksPlugin plugin, RankSerialized serialized) {
    if (serialized.getNext() == null || serialized.getNext().isEmpty()) {
      plugin.getLogger().warning("Having a final rank (for example: \"Z: rank: 'Z'\") from 3.4.2 or earlier should no longer be used.");
      plugin.getLogger().warning("It is safe to just delete the final rank " + serialized.getRank() + "");
      plugin.getLogger().warning("Rankup section '" + serialized.getRank() + "' has a blank 'next' field, will be ignored.");
      return null;
    }

    return new Rankup(serialized.getMessagesAsSection(),
        plugin,
        serialized.getNext(),
        serialized.getRank(),
        serialized.getDisplayName(),
        RankRequirementsFactory.getRequirements(plugin, serialized.getRequirements(), serialized.getPrestigeRequirements()),
        Objects.requireNonNull(serialized.getCommands(), "rank commands are null"));
  }

  protected Rankup(ConfigurationSection section, ArkonasRanksPlugin plugin, String next, String rank, String displayName,
      RankRequirements requirements,
      List<String> commands) {
    super(section, plugin, next, rank, displayName, requirements, commands);
  }
}
