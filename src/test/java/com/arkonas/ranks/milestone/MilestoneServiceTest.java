package com.arkonas.ranks.milestone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.data.RankupRecord;
import com.arkonas.ranks.milestone.MilestoneService.Tier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Pure milestone selection logic (no Bukkit scheduling). */
class MilestoneServiceTest {

  private static RankupRecord rankup() {
    return new RankupRecord(UUID.randomUUID(), "Steve", RankupRecord.Type.RANKUP, "A", "B", 0L);
  }

  private static RankupRecord prestige() {
    return new RankupRecord(UUID.randomUUID(), "Steve", RankupRecord.Type.PRESTIGE, "P", "Q", 0L);
  }

  // --- Tier -----------------------------------------------------------------------------------

  @Test
  void specificCountFires() {
    Tier tier = new Tier(0, List.of(), Map.of(10, List.of("cmdA")));
    assertEquals(List.of("cmdA"), tier.commandsFor(10));
    assertTrue(tier.commandsFor(9).isEmpty());
  }

  @Test
  void everyCadenceFires() {
    Tier tier = new Tier(25, List.of("cmdE"), Map.of());
    assertEquals(List.of("cmdE"), tier.commandsFor(25));
    assertEquals(List.of("cmdE"), tier.commandsFor(50));
    assertTrue(tier.commandsFor(26).isEmpty());
    assertTrue(tier.commandsFor(0).isEmpty(), "count 0 must not fire the cadence");
  }

  @Test
  void specificWinsOverEvery() {
    Tier tier = new Tier(10, List.of("cmdE"), Map.of(50, List.of("cmdS")));
    assertEquals(List.of("cmdS"), tier.commandsFor(50)); // 50 in 'at' and 50 % 10 == 0
    assertEquals(List.of("cmdE"), tier.commandsFor(20)); // only the cadence
  }

  // --- commandsForRecord ----------------------------------------------------------------------

  @Test
  void rankupUsesRankupTierAndCount() {
    MilestoneService service = new MilestoneService(null, true,
        new Tier(25, List.of("R"), Map.of()),
        new Tier(0, List.of(), Map.of()));

    assertEquals(List.of("R"), service.commandsForRecord(rankup(), 25, 3));
    assertTrue(service.commandsForRecord(rankup(), 24, 3).isEmpty());
  }

  @Test
  void prestigeUsesPrestigeTierAndCount() {
    MilestoneService service = new MilestoneService(null, true,
        new Tier(0, List.of(), Map.of()),
        new Tier(0, List.of(), Map.of(5, List.of("P"))));

    assertEquals(List.of("P"), service.commandsForRecord(prestige(), 999, 5));
    assertTrue(service.commandsForRecord(prestige(), 999, 4).isEmpty());
  }

  @Test
  void disabledFiresNothing() {
    MilestoneService service = new MilestoneService(null, false,
        new Tier(1, List.of("R"), Map.of()), new Tier(0, List.of(), Map.of()));
    assertFalse(service.isEnabled());
    assertTrue(service.commandsForRecord(rankup(), 10, 0).isEmpty());
  }

  // --- fromConfig -----------------------------------------------------------------------------

  @Test
  void parsesConfig() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("enabled", true);
    config.set("rankup.every", 25);
    config.set("rankup.every-commands", List.of("say %player% hit %count%"));
    config.set("rankup.at.100", List.of("give %player% diamond"));

    MilestoneService service = MilestoneService.fromConfig(null, config);
    assertTrue(service.isEnabled());
    assertEquals(List.of("say %player% hit %count%"), service.commandsForRecord(rankup(), 25, 0));
    assertEquals(List.of("give %player% diamond"), service.commandsForRecord(rankup(), 100, 0));
    assertTrue(service.commandsForRecord(rankup(), 7, 0).isEmpty());
  }

  @Test
  void disabledConfig() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("enabled", false);
    assertFalse(MilestoneService.fromConfig(null, config).isEnabled());
  }
}
