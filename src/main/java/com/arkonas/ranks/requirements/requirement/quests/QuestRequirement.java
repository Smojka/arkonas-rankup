package com.arkonas.ranks.requirements.requirement.quests;

import java.util.Locale;
import java.util.Set;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

/**
 * Requirement satisfied once the player has completed a given PikaMug Quests quest. Authored as
 * {@code quest <id or name>} — the whole value is one quest id (quest names may contain spaces, so it
 * is not split). Matched case-insensitively. Backed by the reflection {@link QuestsCompletion}
 * adapter (no compile dependency); the membership test ({@link #matches}) is pure and unit-tested.
 */
public class QuestRequirement extends Requirement {

  private static volatile QuestsCompletion completion;

  public QuestRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "quest");
  }

  protected QuestRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    return matches(completion().completedIds(player), getValueString());
  }

  /** True if the completed set contains the required quest id. Case-insensitive; pure. */
  static boolean matches(Set<String> completedQuests, String questId) {
    return questId != null
        && completedQuests.contains(questId.trim().toLowerCase(Locale.ROOT));
  }

  private static QuestsCompletion completion() {
    QuestsCompletion local = completion;
    if (local == null) {
      local = QuestsCompletion.create();
      completion = local;
    }
    return local;
  }

  @Override
  public double getTotal(Player player) {
    return 1;
  }

  @Override
  public Requirement clone() {
    return new QuestRequirement(this);
  }
}
