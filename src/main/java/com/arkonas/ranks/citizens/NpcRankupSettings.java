package com.arkonas.ranks.citizens;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Maps Citizens NPC ids to the command lines run when a player right-clicks them. NPCs not listed
 * fall back to {@code default-commands} (empty = ignore). Values may be a single command string or a
 * list. Pure config holder so the parsing/lookup is unit-tested without Citizens on the classpath.
 */
public final class NpcRankupSettings {

  private final boolean enabled;
  private final Map<Integer, List<String>> commands;
  private final List<String> defaultCommands;

  public NpcRankupSettings(boolean enabled, Map<Integer, List<String>> commands,
      List<String> defaultCommands) {
    this.enabled = enabled;
    this.commands = commands;
    this.defaultCommands = defaultCommands;
  }

  public static NpcRankupSettings fromConfig(ConfigurationSection section) {
    if (section == null || !section.getBoolean("enabled", false)) {
      return new NpcRankupSettings(false, Map.of(), List.of());
    }
    List<String> defaults = section.getStringList("default-commands");
    Map<Integer, List<String>> commands = new LinkedHashMap<>();
    ConfigurationSection npcs = section.getConfigurationSection("npcs");
    if (npcs != null) {
      for (String key : npcs.getKeys(false)) {
        try {
          int id = Integer.parseInt(key.trim());
          List<String> lines = npcs.isList(key)
              ? npcs.getStringList(key)
              : List.of(npcs.getString(key, ""));
          commands.put(id, lines);
        } catch (NumberFormatException ignored) {
          // skip non-numeric NPC keys
        }
      }
    }
    return new NpcRankupSettings(true, commands, defaults);
  }

  public boolean isEnabled() {
    return enabled;
  }

  /** Command lines for an NPC id, falling back to the defaults; empty means "do nothing". */
  public List<String> commandsFor(int npcId) {
    return commands.getOrDefault(npcId, defaultCommands);
  }
}
