package com.arkonas.ranks.requirements.requirement.worldguard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure region-membership logic (the WorldGuard query itself is a live-validate reflection seam). */
class WorldGuardRegionRequirementTest {

  @Test
  void insideRequiredRegionPasses() {
    assertTrue(WorldGuardRegionRequirement.matches(Set.of("spawn"), new String[]{"spawn"}));
  }

  @Test
  void outsideRequiredRegionFails() {
    assertFalse(WorldGuardRegionRequirement.matches(Set.of("wild"), new String[]{"spawn"}));
  }

  @Test
  void anyOfSeveralRequiredRegionsPasses() {
    // "region spawn vip mine" -> in any one of them
    assertTrue(WorldGuardRegionRequirement.matches(
        Set.of("mine"), new String[]{"spawn", "vip", "mine"}));
  }

  @Test
  void matchIsCaseInsensitive() {
    // player region ids come lowercased from the adapter; config value may be mixed case
    assertTrue(WorldGuardRegionRequirement.matches(Set.of("spawn"), new String[]{"SpAwN"}));
  }

  @Test
  void noRegionsAtLocationFails() {
    assertFalse(WorldGuardRegionRequirement.matches(Set.of(), new String[]{"spawn"}));
  }
}
