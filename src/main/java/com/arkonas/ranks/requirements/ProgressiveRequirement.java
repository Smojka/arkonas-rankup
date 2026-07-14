package com.arkonas.ranks.requirements;

import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;

public abstract class ProgressiveRequirement extends Requirement {
  public ProgressiveRequirement(ArkonasRanksPlugin plugin, String name) {
    super(plugin, name);
  }

  public ProgressiveRequirement(ArkonasRanksPlugin plugin, String name, boolean subRequirement) {
    super(plugin, name, subRequirement);
  }

  protected ProgressiveRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    return getRemaining(player) <= 0;
  }

  @Override
  public double getRemaining(Player player) {
    try {
      // both getTotal and getProgress can touch a plugin/PlaceholderAPI hook (e.g.
      // PlaceholderRequirement.getTotal resolves a placeholder that may not be loaded yet), so both
      // are guarded. Fail closed: any throw reads as "requirement unmet" instead of erroring out of
      // /rankup, the menus or a placeholder render.
      return Math.max(0, getTotal(player) - getProgress(player));
    } catch (Throwable t) {
      logHookFailureOnce(t);
      return 1; // positive => still remaining => unmet
    }
  }

  @Override
  public double getTotal(Player player) {
    return getValueDouble();
  }

  public abstract double getProgress(Player player);
}
