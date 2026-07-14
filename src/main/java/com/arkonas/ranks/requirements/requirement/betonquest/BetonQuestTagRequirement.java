package com.arkonas.ranks.requirements.requirement.betonquest;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

/**
 * Requirement satisfied when the player holds a given BetonQuest tag ({@code betonquest-tag <tag>}).
 * The whole value is one tag (BetonQuest tags may be package-prefixed). Backed by the reflection
 * {@link BetonQuestTags} adapter; no compile dependency, fails closed.
 */
public class BetonQuestTagRequirement extends Requirement {

  private static volatile BetonQuestTags tags;

  public BetonQuestTagRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "betonquest-tag");
  }

  protected BetonQuestTagRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    String tag = getValueString();
    return tag != null && tags().hasTag(player, tag.trim());
  }

  private static BetonQuestTags tags() {
    BetonQuestTags local = tags;
    if (local == null) {
      local = BetonQuestTags.create();
      tags = local;
    }
    return local;
  }

  @Override
  public double getTotal(Player player) {
    return 1;
  }

  @Override
  public Requirement clone() {
    return new BetonQuestTagRequirement(this);
  }
}
