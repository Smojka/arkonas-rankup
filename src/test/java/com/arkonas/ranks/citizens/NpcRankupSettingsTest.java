package com.arkonas.ranks.citizens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Pure NPC id -> command mapping (list/scalar values, default fallback, disabled). */
class NpcRankupSettingsTest {

  @Test
  void parsesListAndScalarValuesWithDefault() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("enabled", true);
    config.set("default-commands", List.of("rankup"));
    config.set("npcs.0", "maxrankup");                       // scalar
    config.set("npcs.7", List.of("prestige", "[console] say hi")); // list

    NpcRankupSettings settings = NpcRankupSettings.fromConfig(config);
    assertTrue(settings.isEnabled());
    assertEquals(List.of("maxrankup"), settings.commandsFor(0));
    assertEquals(List.of("prestige", "[console] say hi"), settings.commandsFor(7));
    assertEquals(List.of("rankup"), settings.commandsFor(99)); // unlisted -> default
  }

  @Test
  void noDefaultMeansUnlistedNpcsAreIgnored() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("enabled", true);
    config.set("npcs.3", "rankup");

    NpcRankupSettings settings = NpcRankupSettings.fromConfig(config);
    assertEquals(List.of("rankup"), settings.commandsFor(3));
    assertTrue(settings.commandsFor(4).isEmpty());
  }

  @Test
  void nonNumericNpcKeysSkipped() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("enabled", true);
    config.set("npcs.abc", "rankup");
    config.set("npcs.2", "prestige");

    NpcRankupSettings settings = NpcRankupSettings.fromConfig(config);
    assertEquals(List.of("prestige"), settings.commandsFor(2));
    assertTrue(settings.commandsFor(0).isEmpty());
  }

  @Test
  void disabledConfigIsInert() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("enabled", false);
    NpcRankupSettings settings = NpcRankupSettings.fromConfig(config);
    assertFalse(settings.isEnabled());
    assertTrue(settings.commandsFor(0).isEmpty());
  }

  @Test
  void nullSectionIsInert() {
    assertFalse(NpcRankupSettings.fromConfig(null).isEnabled());
  }
}
