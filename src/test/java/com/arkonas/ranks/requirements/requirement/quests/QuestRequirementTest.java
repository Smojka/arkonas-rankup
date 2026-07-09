package com.arkonas.ranks.requirements.requirement.quests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure completed-quest membership (the Quests API query itself is a live-validate reflection seam). */
class QuestRequirementTest {

  @Test
  void completedQuestPasses() {
    assertTrue(QuestRequirement.matches(Set.of("first steps"), "First Steps"));
  }

  @Test
  void uncompletedQuestFails() {
    assertFalse(QuestRequirement.matches(Set.of("first steps"), "Dragon Slayer"));
  }

  @Test
  void matchIsCaseInsensitiveAndTrimmed() {
    assertTrue(QuestRequirement.matches(Set.of("mine 100 blocks"), "  MINE 100 Blocks "));
  }

  @Test
  void noCompletedQuestsFails() {
    assertFalse(QuestRequirement.matches(Set.of(), "First Steps"));
  }

  @Test
  void nullQuestIdFails() {
    assertFalse(QuestRequirement.matches(Set.of("first steps"), null));
  }
}
