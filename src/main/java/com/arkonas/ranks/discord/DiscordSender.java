package com.arkonas.ranks.discord;

/**
 * Sends a plain-text message to a named Discord channel. Implemented by a plugin adapter
 * (currently {@link DiscordSrvSender}); a null sender means no Discord plugin is present, in
 * which case {@link DiscordAnnouncer} degrades to a no-op.
 */
@FunctionalInterface
public interface DiscordSender {

  /**
   * @param channel the game-channel name to route to (e.g. {@code global}); the adapter maps it
   *                to the linked Discord text channel
   * @param message the already-rendered message text
   */
  void send(String channel, String message);
}
