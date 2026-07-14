package com.arkonas.ranks.discord;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Announces rankups and prestiges to Discord. Holds the configured channel and message templates
 * and delegates the actual delivery to a {@link DiscordSender} adapter, so the selection/rendering
 * logic is unit-testable without a live server. Disabled by default; also inert when no sender is
 * available (DiscordSRV absent), so it never throws on servers without the dependency.
 */
public final class DiscordAnnouncer {

  private final boolean enabled;
  private final String channel;
  private final String rankupTemplate;
  private final String prestigeTemplate;
  private final DiscordSender sender;

  public DiscordAnnouncer(boolean enabled, String channel, String rankupTemplate,
      String prestigeTemplate, DiscordSender sender) {
    this.enabled = enabled;
    this.channel = channel;
    this.rankupTemplate = rankupTemplate;
    this.prestigeTemplate = prestigeTemplate;
    this.sender = sender;
  }

  public static DiscordAnnouncer fromConfig(ConfigurationSection section, DiscordSender sender) {
    if (section == null || !section.getBoolean("enabled", false)) {
      return new DiscordAnnouncer(false, "", "", "", sender);
    }
    return new DiscordAnnouncer(true,
        section.getString("channel", "global"),
        section.getString("rankup-message", ""),
        section.getString("prestige-message", ""),
        sender);
  }

  /** Live only when enabled in config and a delivery adapter is actually present. */
  public boolean isEnabled() {
    return enabled && sender != null;
  }

  public void announceRankup(String player, String from, String to) {
    dispatch(rankupTemplate, player, from, to, "rankup");
  }

  public void announcePrestige(String player, String from, String to) {
    dispatch(prestigeTemplate, player, from, to, "prestige");
  }

  private void dispatch(String template, String player, String from, String to, String type) {
    if (!isEnabled() || template == null || template.isBlank()) {
      return;
    }
    sender.send(channel, render(template, player, from, to, type));
  }

  /** Substitutes the message placeholders. Package-private so the test can exercise it directly. */
  static String render(String template, String player, String from, String to, String type) {
    return template
        .replace("%player%", player == null ? "" : player)
        .replace("%from%", from == null ? "" : from)
        .replace("%to%", to == null ? "" : to)
        .replace("%type%", type);
  }
}
