package com.arkonas.ranks.citizens;

import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import com.arkonas.ranks.ArkonasRanksPlugin;

/**
 * Binds right-clicks on Citizens NPCs to configured commands. Registered entirely by reflection —
 * the plugin has no Citizens compile dependency. Rather than listen to a vanilla event (which
 * player-type NPCs don't reliably fire), it registers a handler for Citizens' own
 * {@code NPCRightClickEvent} via {@link org.bukkit.plugin.PluginManager#registerEvent}, so every
 * NPC type is covered. All method handles are resolved up front; anything missing makes
 * {@link #register} return false and the hook is simply skipped.
 *
 * <p>Reflection + live-event only: cannot be exercised by the test suite (the command routing it
 * calls into — {@link NpcRankupSettings}, {@link NpcCommand} — is tested separately). Validate on a
 * live server with Citizens installed.
 */
public final class CitizensHook {

  private CitizensHook() {
  }

  public static boolean register(ArkonasRanksPlugin plugin, NpcRankupSettings settings) {
    if (!settings.isEnabled() || !Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
      return false;
    }
    try {
      Class<?> eventClass = Class.forName("net.citizensnpcs.api.event.NPCRightClickEvent");
      Method getNpc = findMethod(eventClass, "getNPC");
      Method getClicker = findMethod(eventClass, "getClicker");
      Method getId = Class.forName("net.citizensnpcs.api.npc.NPC").getMethod("getId");
      if (getNpc == null || getClicker == null) {
        return false;
      }
      EventExecutor executor = (listener, event) -> {
        try {
          Object npc = getNpc.invoke(event);
          if (npc == null) {
            return;
          }
          List<String> commands = settings.commandsFor((int) getId.invoke(npc));
          if (commands.isEmpty()) {
            return;
          }
          if (getClicker.invoke(event) instanceof Player player) {
            run(player, commands);
          }
        } catch (Throwable ignored) {
          // never let an NPC interaction disrupt the server
        }
      };
      // ignoreCancelled=true: skip clicks another plugin (protection/region) already cancelled
      Bukkit.getPluginManager().registerEvent(eventClass.asSubclass(Event.class),
          new Listener() { }, EventPriority.NORMAL, executor, plugin, true);
      return true;
    } catch (Throwable t) {
      plugin.getLogger().warning("Citizens hook unavailable, NPC rankup disabled: " + t);
      return false;
    }
  }

  private static Method findMethod(Class<?> type, String name) {
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      try {
        return c.getMethod(name);
      } catch (NoSuchMethodException ignored) {
        // walk up to the click-event superclass
      }
    }
    return null;
  }

  private static void run(Player player, List<String> commands) {
    for (String line : commands) {
      NpcCommand.Dispatch dispatch = NpcCommand.parse(line, player.getName());
      if (dispatch.command().isBlank()) {
        continue;
      }
      if (dispatch.console()) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), dispatch.command());
      } else {
        player.performCommand(dispatch.command());
      }
    }
  }
}
