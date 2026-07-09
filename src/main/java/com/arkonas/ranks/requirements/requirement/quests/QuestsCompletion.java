package com.arkonas.ranks.requirements.requirement.quests;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Resolves the set of quest ids/names a player has completed in PikaMug's Quests plugin, entirely by
 * reflection so there is no Quests compile dependency. The plugin instance, {@code getQuester(UUID)}
 * and {@code getCompletedQuests()} handles are resolved up front; the per-element id getter is
 * discovered on first use because completed quests are returned as either Strings or Quest objects
 * across versions. Anything missing marks the adapter unavailable and
 * {@link #completedIds(Player)} returns an empty set (so a quest requirement it backs just never
 * passes rather than throwing).
 *
 * <p>Reflection-only: cannot be exercised by the test suite; validate on a live server. The pure
 * membership test that consumes this lives in {@code QuestRequirement#matches}.
 */
public final class QuestsCompletion {

  private final boolean available;
  private final Object questsPlugin;
  private final Method getQuester;
  private final Method getCompletedQuests;
  private volatile Method idGetter; // resolved from the first quest element seen

  private QuestsCompletion(boolean available, Object questsPlugin, Method getQuester,
      Method getCompletedQuests) {
    this.available = available;
    this.questsPlugin = questsPlugin;
    this.getQuester = getQuester;
    this.getCompletedQuests = getCompletedQuests;
  }

  public static QuestsCompletion create() {
    Object plugin = Bukkit.getPluginManager().getPlugin("Quests");
    if (plugin == null || !Bukkit.getPluginManager().isPluginEnabled("Quests")) {
      return unavailable();
    }
    try {
      Method getQuester = findMethod(plugin.getClass(), "getQuester", java.util.UUID.class);
      if (getQuester == null) {
        return unavailable();
      }
      Class<?> questerType = getQuester.getReturnType();
      Method getCompletedQuests = questerType.getMethod("getCompletedQuests");
      return new QuestsCompletion(true, plugin, getQuester, getCompletedQuests);
    } catch (Throwable t) {
      return unavailable();
    }
  }

  private static QuestsCompletion unavailable() {
    return new QuestsCompletion(false, null, null, null);
  }

  /** Lowercased ids/names of the player's completed quests; empty if unavailable. */
  public Set<String> completedIds(Player player) {
    if (!available) {
      return Set.of();
    }
    try {
      Object quester = getQuester.invoke(questsPlugin, player.getUniqueId());
      if (quester == null) {
        return Set.of();
      }
      Object completed = getCompletedQuests.invoke(quester);
      Set<String> ids = new HashSet<>();
      if (completed instanceof Iterable<?> iterable) {
        for (Object element : iterable) {
          String id = idOf(element);
          if (id != null) {
            ids.add(id.toLowerCase(Locale.ROOT));
          }
        }
      }
      return ids;
    } catch (Throwable t) {
      return Set.of();
    }
  }

  private String idOf(Object element) {
    if (element == null) {
      return null;
    }
    if (element instanceof String string) {
      return string;
    }
    try {
      Method getter = idGetter;
      if (getter == null) {
        getter = resolveIdGetter(element.getClass());
        idGetter = getter;
      }
      return getter == null ? String.valueOf(element) : String.valueOf(getter.invoke(element));
    } catch (Throwable t) {
      return null;
    }
  }

  private static Method resolveIdGetter(Class<?> type) {
    for (String name : new String[] {"getId", "getName"}) {
      try {
        return type.getMethod(name);
      } catch (NoSuchMethodException ignored) {
        // try the next candidate
      }
    }
    return null;
  }

  private static Method findMethod(Class<?> type, String name, Class<?>... params) {
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      try {
        return c.getMethod(name, params);
      } catch (NoSuchMethodException ignored) {
        // walk up the hierarchy / interfaces
      }
    }
    for (Class<?> iface : type.getInterfaces()) {
      try {
        return iface.getMethod(name, params);
      } catch (NoSuchMethodException ignored) {
        // continue
      }
    }
    return null;
  }
}
