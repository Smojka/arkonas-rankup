package com.arkonas.ranks.milestone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.data.MilestoneHook;
import com.arkonas.ranks.data.RankupRecord;

/**
 * Runs reward commands when a player's cumulative rankup or prestige count reaches a milestone —
 * either a specific configured count or a "every N" cadence. Wired as the {@link MilestoneHook} on
 * the stats service, so the count it sees is the just-written total. Reward dispatch hops to the
 * main thread. Disabled by default.
 */
public final class MilestoneService implements MilestoneHook {

  /** A milestone table for one counter: specific counts win over the "every N" cadence. */
  public static final class Tier {
    private final int every;
    private final List<String> everyCommands;
    private final Map<Integer, List<String>> at;

    public Tier(int every, List<String> everyCommands, Map<Integer, List<String>> at) {
      this.every = Math.max(0, every);
      this.everyCommands = everyCommands;
      this.at = at;
    }

    /** Commands for a count, or empty if nothing fires. Specific counts take precedence. */
    public List<String> commandsFor(int count) {
      List<String> specific = at.get(count);
      if (specific != null) {
        return specific;
      }
      if (every > 0 && count > 0 && count % every == 0) {
        return everyCommands;
      }
      return List.of();
    }
  }

  private final ArkonasRanksPlugin plugin;
  private final boolean enabled;
  private final Tier rankup;
  private final Tier prestige;

  public MilestoneService(ArkonasRanksPlugin plugin, boolean enabled, Tier rankup, Tier prestige) {
    this.plugin = plugin;
    this.enabled = enabled;
    this.rankup = rankup;
    this.prestige = prestige;
  }

  public static MilestoneService fromConfig(ArkonasRanksPlugin plugin, ConfigurationSection section) {
    if (section == null || !section.getBoolean("enabled", false)) {
      return new MilestoneService(plugin, false, emptyTier(), emptyTier());
    }
    return new MilestoneService(plugin, true,
        tier(section.getConfigurationSection("rankup")),
        tier(section.getConfigurationSection("prestige")));
  }

  public boolean isEnabled() {
    return enabled;
  }

  private static Tier emptyTier() {
    return new Tier(0, List.of(), Map.of());
  }

  private static Tier tier(ConfigurationSection section) {
    if (section == null) {
      return emptyTier();
    }
    int every = section.getInt("every", 0);
    List<String> everyCommands = section.getStringList("every-commands");
    Map<Integer, List<String>> at = new LinkedHashMap<>();
    ConfigurationSection atSection = section.getConfigurationSection("at");
    if (atSection != null) {
      for (String key : atSection.getKeys(false)) {
        try {
          at.put(Integer.parseInt(key.trim()), atSection.getStringList(key));
        } catch (NumberFormatException ignored) {
          // skip non-numeric milestone keys
        }
      }
    }
    return new Tier(every, everyCommands, at);
  }

  private static boolean isPrestige(RankupRecord record) {
    return record.type() == RankupRecord.Type.PRESTIGE
        || record.type() == RankupRecord.Type.FORCE_PRESTIGE;
  }

  /** Pure selection: the commands that should fire for a record + totals, or empty. */
  List<String> commandsForRecord(RankupRecord record, int rankupCount, int prestigeCount) {
    if (!enabled) {
      return List.of();
    }
    Tier tier = isPrestige(record) ? prestige : rankup;
    int count = isPrestige(record) ? prestigeCount : rankupCount;
    return tier.commandsFor(count);
  }

  @Override
  public void onRecord(RankupRecord record, int rankupCount, int prestigeCount) {
    List<String> commands = commandsForRecord(record, rankupCount, prestigeCount);
    if (commands.isEmpty()) {
      return;
    }
    boolean isPrestige = isPrestige(record);
    int count = isPrestige ? prestigeCount : rankupCount;
    // this runs on the stats thread; dispatch commands on the main thread
    Bukkit.getScheduler().runTask(plugin,
        () -> dispatch(record.name(), count, isPrestige, commands));
  }

  private void dispatch(String playerName, int count, boolean isPrestige, List<String> commands) {
    List<String> rendered = new ArrayList<>(commands.size());
    for (String command : commands) {
      rendered.add(command
          .replace("%player%", playerName)
          .replace("%count%", String.valueOf(count))
          .replace("%type%", isPrestige ? "prestige" : "rankup"));
    }
    for (String command : rendered) {
      if (!command.isBlank()) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
      }
    }
  }
}
