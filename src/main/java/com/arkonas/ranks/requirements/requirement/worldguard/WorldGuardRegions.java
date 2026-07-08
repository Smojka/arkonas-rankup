package com.arkonas.ranks.requirements.requirement.worldguard;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.entity.Player;

/**
 * Resolves the WorldGuard region ids a player is currently standing in, entirely by reflection so
 * the plugin has no WorldGuard compile dependency. All method handles are resolved up front in
 * {@link #create()}; if anything is missing the adapter is marked unavailable and
 * {@link #regionsAt(Player)} returns an empty set (so a region requirement it backs simply never
 * passes rather than throwing).
 *
 * <p>Reflection-only: cannot be exercised by the test suite; validate on a live server. The pure
 * membership logic that consumes this lives in {@code WorldGuardRegionRequirement#matches}.
 */
public final class WorldGuardRegions {

  private final boolean available;
  private final Object container;
  private final Method createQuery;
  private final Method getApplicableRegions;
  private final Method adapt;
  private final Method getRegions;
  private final Method getId;

  private WorldGuardRegions(boolean available, Object container, Method createQuery,
      Method getApplicableRegions, Method adapt, Method getRegions, Method getId) {
    this.available = available;
    this.container = container;
    this.createQuery = createQuery;
    this.getApplicableRegions = getApplicableRegions;
    this.adapt = adapt;
    this.getRegions = getRegions;
    this.getId = getId;
  }

  public static WorldGuardRegions create() {
    try {
      Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
      Object instance = worldGuardClass.getMethod("getInstance").invoke(null);
      Object platform = worldGuardClass.getMethod("getPlatform").invoke(instance);
      Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);

      Class<?> weLocation = Class.forName("com.sk89q.worldedit.util.Location");
      Method createQuery = container.getClass().getMethod("createQuery");
      Class<?> queryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
      Method getApplicableRegions = queryClass.getMethod("getApplicableRegions", weLocation);
      Method adapt = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter")
          .getMethod("adapt", org.bukkit.Location.class);
      Class<?> applicableSet =
          Class.forName("com.sk89q.worldguard.protection.ApplicableRegionSet");
      Method getRegions = applicableSet.getMethod("getRegions");
      Method getId = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion")
          .getMethod("getId");
      return new WorldGuardRegions(true, container, createQuery, getApplicableRegions, adapt,
          getRegions, getId);
    } catch (Throwable t) {
      return new WorldGuardRegions(false, null, null, null, null, null, null);
    }
  }

  /** Lowercased ids of the regions at the player's current location; empty if unavailable. */
  public Set<String> regionsAt(Player player) {
    if (!available) {
      return Set.of();
    }
    try {
      Object location = adapt.invoke(null, player.getLocation());
      Object query = createQuery.invoke(container);
      Object applicable = getApplicableRegions.invoke(query, location);
      Set<String> ids = new HashSet<>();
      for (Object region : (Iterable<?>) getRegions.invoke(applicable)) {
        ids.add(((String) getId.invoke(region)).toLowerCase(Locale.ROOT));
      }
      return ids;
    } catch (Throwable t) {
      return Set.of();
    }
  }
}
