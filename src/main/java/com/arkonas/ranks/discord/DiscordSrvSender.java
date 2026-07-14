package com.arkonas.ranks.discord;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

/**
 * {@link DiscordSender} backed by DiscordSRV, via reflection so the plugin compiles without a
 * DiscordSRV dependency. All method handles are resolved up front in {@link #tryCreate(Logger)};
 * a missing plugin or a differing API shape makes it return null, and the caller falls back to a
 * no-op. The JDA {@code sendMessage} overload is discovered by shape (a static 2-arg method whose
 * second parameter is a String) so it survives the JDA package moves between DiscordSRV versions.
 *
 * <p>Reflection-only: cannot be exercised by the test suite; validate on a live server.
 */
public final class DiscordSrvSender implements DiscordSender {

  private final Object discordSrv;
  private final Method getChannel;
  private final Method sendMessage;

  private DiscordSrvSender(Object discordSrv, Method getChannel, Method sendMessage) {
    this.discordSrv = discordSrv;
    this.getChannel = getChannel;
    this.sendMessage = sendMessage;
  }

  public static DiscordSender tryCreate(Logger logger) {
    if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
      return null;
    }
    try {
      Class<?> discordSrvClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
      Object instance = discordSrvClass.getMethod("getPlugin").invoke(null);
      Method getChannel = discordSrvClass.getMethod(
          "getDestinationTextChannelForGameChannelName", String.class);
      Class<?> util = Class.forName("github.scarsz.discordsrv.util.DiscordUtil");
      Method sendMessage = null;
      for (Method method : util.getMethods()) {
        if (method.getName().equals("sendMessage") && method.getParameterCount() == 2
            && method.getParameterTypes()[1] == String.class) {
          sendMessage = method;
          break;
        }
      }
      if (instance == null || sendMessage == null) {
        return null;
      }
      return new DiscordSrvSender(instance, getChannel, sendMessage);
    } catch (Throwable t) {
      if (logger != null) {
        logger.warning("DiscordSRV hook unavailable, announcements disabled: " + t);
      }
      return null;
    }
  }

  @Override
  public void send(String channel, String message) {
    try {
      Object textChannel = getChannel.invoke(discordSrv, channel);
      if (textChannel == null) {
        return; // channel not linked in DiscordSRV config
      }
      sendMessage.invoke(null, textChannel, message);
    } catch (Throwable ignored) {
      // best effort: never let an announcement failure disrupt a rankup
    }
  }
}
