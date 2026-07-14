package com.arkonas.ranks.rebirth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.prestige.Prestige;
import com.arkonas.ranks.prestige.Prestiges;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.ranks.requirements.ListRankRequirements;
import com.arkonas.ranks.ranks.requirements.RankRequirements;
import com.arkonas.ranks.requirements.Requirement;

/**
 * The rebirth tier: an optional progression layer above rankup (and prestige). A player who has
 * reached the top of their progression can "be reborn" — advancing through an ordered list of
 * rebirth permission groups, resetting back to the first rank, in exchange for requirements and
 * reward commands. Rebirth is a single global track, disabled by default.
 *
 * <p>Self-contained: its messages live in its own config block, so enabling it touches no locale
 * files and leaves the chat behaviour untouched.</p>
 */
public final class RebirthManager {

  private final ArkonasRanksPlugin plugin;
  private final boolean enabled;
  private final boolean requiresTopPrestige;
  private final List<String> groups;
  private final boolean resetRanks;
  private final boolean resetPrestige;
  private final RankRequirements requirements;
  private final List<String> commands;
  private final Map<String, String> messages;

  private RebirthManager(ArkonasRanksPlugin plugin, boolean enabled, boolean requiresTopPrestige,
      List<String> groups, boolean resetRanks, boolean resetPrestige, RankRequirements requirements,
      List<String> commands, Map<String, String> messages) {
    this.plugin = plugin;
    this.enabled = enabled;
    this.requiresTopPrestige = requiresTopPrestige;
    this.groups = groups;
    this.resetRanks = resetRanks;
    this.resetPrestige = resetPrestige;
    this.requirements = requirements;
    this.commands = commands;
    this.messages = messages;
  }

  public static RebirthManager disabled(ArkonasRanksPlugin plugin) {
    return new RebirthManager(plugin, false, false, List.of(), true, false,
        new ListRankRequirements(List.of()), List.of(), defaultMessages());
  }

  public static RebirthManager fromConfig(ArkonasRanksPlugin plugin, ConfigurationSection section) {
    if (section == null || !section.getBoolean("enabled", false)) {
      return disabled(plugin);
    }
    boolean topPrestige = "top-prestige".equalsIgnoreCase(section.getString("requires", "top-rank"));
    List<String> groups = new ArrayList<>();
    for (String group : section.getStringList("groups")) {
      groups.add(group);
    }
    if (groups.isEmpty()) {
      plugin.getLogger().warning("rebirth is enabled but has no 'groups'; disabling rebirth.");
      return disabled(plugin);
    }
    boolean resetRanks = section.getBoolean("reset-ranks", true);
    boolean resetPrestige = section.getBoolean("reset-prestige", false);
    List<Requirement> reqs = plugin.getRequirements().getRequirements(section.getStringList("requirements"));
    List<String> commands = section.getStringList("commands");

    Map<String, String> messages = defaultMessages();
    ConfigurationSection messageSection = section.getConfigurationSection("messages");
    if (messageSection != null) {
      for (String key : messageSection.getKeys(false)) {
        messages.put(key.toLowerCase(Locale.ROOT), messageSection.getString(key));
      }
    }
    return new RebirthManager(plugin, true, topPrestige, groups, resetRanks, resetPrestige,
        new ListRankRequirements(reqs), commands, messages);
  }

  private static Map<String, String> defaultMessages() {
    Map<String, String> messages = new LinkedHashMap<>();
    messages.put("success", "&aYou have been reborn as &e{rebirth}&a!");
    messages.put("requirements-not-met", "&cYou do not meet the requirements to be reborn.");
    messages.put("not-at-top", "&cYou must reach the top of your progression before you can be reborn.");
    messages.put("maxed", "&cYou have reached the maximum rebirth.");
    messages.put("disabled", "&cRebirth is not enabled on this server.");
    return messages;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public List<String> getGroups() {
    return groups;
  }

  /** Index of the player's highest rebirth group, or -1 if they have not been reborn. */
  public int currentIndex(Player player) {
    for (int i = groups.size() - 1; i >= 0; i--) {
      if (plugin.getPermissions().inGroup(player.getUniqueId(), groups.get(i))) {
        return i;
      }
    }
    return -1;
  }

  public String currentGroup(Player player) {
    int index = currentIndex(player);
    return index < 0 ? null : groups.get(index);
  }

  public String nextGroup(Player player) {
    int index = currentIndex(player);
    return index + 1 < groups.size() ? groups.get(index + 1) : null;
  }

  public boolean isMaxed(Player player) {
    return currentIndex(player) >= groups.size() - 1;
  }

  /** Whether the player has reached the top of the gating track (top rank, or top prestige). */
  public boolean isAtTop(Player player) {
    if (requiresTopPrestige && plugin.getPrestiges() != null) {
      Prestiges prestiges = plugin.getPrestiges();
      RankElement<Prestige> element = prestiges.getByPlayer(player);
      return element != null && !element.hasNext();
    }
    RankElement<Rank> element = plugin.getRankups().getByPlayer(player);
    return element != null && !element.hasNext();
  }

  public boolean canRebirth(Player player) {
    return enabled && !groups.isEmpty() && isAtTop(player) && !isMaxed(player)
        && requirements.hasRequirements(player);
  }

  /**
   * Attempts a rebirth, sending the player the relevant message. Returns true only if the player
   * was reborn.
   */
  public boolean rebirth(Player player) {
    if (!enabled || groups.isEmpty()) {
      message(player, "disabled", null);
      return false;
    }
    if (!isAtTop(player)) {
      message(player, "not-at-top", null);
      return false;
    }
    if (isMaxed(player)) {
      message(player, "maxed", null);
      return false;
    }
    if (!requirements.hasRequirements(player)) {
      message(player, "requirements-not-met", null);
      return false;
    }

    requirements.applyRequirements(player);

    String current = currentGroup(player);
    String next = nextGroup(player);
    plugin.getPermissions().transferGroup(player.getUniqueId(), current, next);

    if (resetRanks) {
      resetToFirstRank(player);
    }
    if (resetPrestige) {
      resetPrestige(player);
    }
    if (plugin.getEffectsListener() != null) {
      plugin.getEffectsListener().celebrateRebirth(player, current, next);
    }
    runCommands(player, next);
    message(player, "success", next);
    return true;
  }

  private void resetToFirstRank(Player player) {
    RankElement<Rank> element = plugin.getRankups().getByPlayer(player);
    if (element == null) {
      return;
    }
    String current = element.getRank().getRank();
    String first = plugin.getRankups().getFirst().getRank();
    if (current != null && first != null && !current.equalsIgnoreCase(first)) {
      plugin.getPermissions().transferGroup(player.getUniqueId(), current, first);
    }
  }

  /** Removes every prestige group the player holds, resetting them to zero prestiges. */
  private void resetPrestige(Player player) {
    Prestiges prestiges = plugin.getPrestiges();
    if (prestiges == null) {
      return;
    }
    for (Prestige prestige : prestiges.getTree()) {
      String group = prestige.getRank();
      if (group != null && plugin.getPermissions().inGroup(player.getUniqueId(), group)) {
        // remove-only: transferGroup requires a non-null target, so use removeGroup here
        plugin.getPermissions().removeGroup(player.getUniqueId(), group);
      }
    }
  }

  private void runCommands(Player player, String rebirthName) {
    for (String command : commands) {
      String rendered = command
          .replace("{player}", player.getName())
          .replace("{rebirth}", rebirthName == null ? "" : rebirthName);
      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
        rendered = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, rendered);
      }
      if (!rendered.isBlank()) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rendered);
      }
    }
  }

  private void message(Player player, String key, String rebirthName) {
    String raw = messages.get(key);
    if (raw == null || raw.isEmpty()) {
      return;
    }
    String rendered = raw
        .replace("{player}", player.getName())
        .replace("{rebirth}", rebirthName == null ? "" : rebirthName);
    player.sendMessage(ChatColor.translateAlternateColorCodes('&', rendered));
  }
}
