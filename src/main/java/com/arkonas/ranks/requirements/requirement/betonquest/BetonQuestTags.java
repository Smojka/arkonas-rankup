package com.arkonas.ranks.requirements.requirement.betonquest;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Checks BetonQuest tags by reflection so there is no BetonQuest compile dependency. Works across
 * the API's major rewrites: it discovers {@code getPlayerData} by shape and picks the profile path
 * (modern, via a {@code PlayerConverter.getID(Player)}) or the legacy string-id path automatically.
 * Anything missing makes it unavailable and {@link #hasTag(Player, String)} returns false (fails
 * closed, so the requirement never wrongly passes).
 *
 * <p>Reflection-only: cannot be exercised by the test suite; validate on a live server.
 */
public final class BetonQuestTags {

  private static final String[] BQ_CLASSES = {
      "org.betonquest.betonquest.BetonQuest", "pl.betoncraft.betonquest.BetonQuest"};
  private static final String[] CONVERTER_CLASSES = {
      "org.betonquest.betonquest.util.PlayerConverter",
      "org.betonquest.betonquest.api.profiles.PlayerConverter",
      "org.betonquest.betonquest.utils.PlayerConverter",
      "pl.betoncraft.betonquest.utils.PlayerConverter"};

  private final boolean available;
  private final Object betonQuest;
  private final Method getId;         // null on the legacy string-id path
  private final Method getPlayerData;
  private final Method hasTag;
  private final boolean legacyStringId;

  private BetonQuestTags(boolean available, Object betonQuest, Method getId, Method getPlayerData,
      Method hasTag, boolean legacyStringId) {
    this.available = available;
    this.betonQuest = betonQuest;
    this.getId = getId;
    this.getPlayerData = getPlayerData;
    this.hasTag = hasTag;
    this.legacyStringId = legacyStringId;
  }

  private static BetonQuestTags unavailable() {
    return new BetonQuestTags(false, null, null, null, null, false);
  }

  public static BetonQuestTags create() {
    if (!Bukkit.getPluginManager().isPluginEnabled("BetonQuest")) {
      return unavailable();
    }
    try {
      Class<?> bqClass = firstClass(BQ_CLASSES);
      if (bqClass == null) {
        return unavailable();
      }
      Object instance = bqClass.getMethod("getInstance").invoke(null);
      Method getPlayerData = firstMethodNamed(bqClass, "getPlayerData", 1);
      if (instance == null || getPlayerData == null) {
        return unavailable();
      }
      Class<?> idParam = getPlayerData.getParameterTypes()[0];
      boolean legacyStringId = idParam == String.class;
      Method getId = null;
      if (!legacyStringId) {
        Class<?> converter = firstClass(CONVERTER_CLASSES);
        if (converter == null) {
          return unavailable();
        }
        getId = converter.getMethod("getID", Player.class);
      }
      Method hasTag = getPlayerData.getReturnType().getMethod("hasTag", String.class);
      return new BetonQuestTags(true, instance, getId, getPlayerData, hasTag, legacyStringId);
    } catch (Throwable t) {
      return unavailable();
    }
  }

  public boolean hasTag(Player player, String tag) {
    if (!available || tag == null) {
      return false;
    }
    try {
      Object id = legacyStringId ? player.getUniqueId().toString() : getId.invoke(null, player);
      Object data = getPlayerData.invoke(betonQuest, id);
      if (data == null) {
        return false;
      }
      return hasTag.invoke(data, tag) instanceof Boolean result && result;
    } catch (Throwable t) {
      return false;
    }
  }

  private static Class<?> firstClass(String... names) {
    for (String name : names) {
      try {
        return Class.forName(name);
      } catch (Throwable ignored) {
        // try the next candidate
      }
    }
    return null;
  }

  private static Method firstMethodNamed(Class<?> type, String name, int paramCount) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
        return method;
      }
    }
    return null;
  }
}
