package com.arkonas.ranks.requirements.requirement.worldguard;

import java.util.Locale;
import java.util.Set;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

/**
 * Requirement satisfied while the player stands inside a WorldGuard region. Authored as
 * {@code region <id>} — or {@code region <id1> <id2> ...} to accept any of several regions (useful
 * for a shared rankup zone). Region ids are matched case-insensitively. Backed by the reflection
 * {@link WorldGuardRegions} adapter (no compile dependency); the membership test itself
 * ({@link #matches}) is pure and unit-tested.
 */
public class WorldGuardRegionRequirement extends Requirement {

  private static volatile WorldGuardRegions regions;

  public WorldGuardRegionRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "region");
  }

  protected WorldGuardRegionRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    return matches(regions().regionsAt(player), getValuesString());
  }

  /** True if the player is in any of the required regions. Case-insensitive; pure. */
  static boolean matches(Set<String> playerRegions, String[] required) {
    for (String id : required) {
      if (playerRegions.contains(id.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  private static WorldGuardRegions regions() {
    WorldGuardRegions local = regions;
    if (local == null) {
      local = WorldGuardRegions.create();
      regions = local;
    }
    return local;
  }

  @Override
  public double getTotal(Player player) {
    return 1;
  }

  @Override
  public Requirement clone() {
    return new WorldGuardRegionRequirement(this);
  }
}
