package com.arkonas.ranks.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ranks.Rank;

public interface MessageBuilder {
  MessageBuilder replaceKey(String key, Object value);
  MessageBuilder replacePlayer(CommandSender sender);
  MessageBuilder replaceRank(Rank rank);
  MessageBuilder replaceOldRank(Rank rank);
  MessageBuilder replaceSeconds(long seconds, long secondsLeft);

  void send(CommandSender sender);
  void broadcast();

  String toString();

  MessageBuilder failIfEmpty();
  MessageBuilder failIf(boolean b);

  default String toString(Player player) {
    return toString();
  }

  /**
   * Renders this message into an Adventure {@link Component} for the given player.
   * The default implementation is a legacy fallback (parses the section-coded
   * {@link #toString(Player)} output); implementations backed by the full text
   * pipeline (see PebbleMessageBuilder) override this to keep hex/MiniMessage.
   */
  default Component toComponent(Player player) {
    return LegacyComponentSerializer.legacySection().deserialize(toString(player));
  }
}
