package com.arkonas.ranks.requirements;

import lombok.Getter;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;

public abstract class Requirement implements Cloneable {

  /**
   * Slack for rounding an integer cost up. Deductions use {@code ceil(cost - EPSILON)} so they can
   * never take less than the affordability check demanded, while a cost that is mathematically whole
   * but lands a hair above it in binary floating point (e.g. {@code 20 * 0.15}) is not charged an
   * extra unit.
   */
  protected static final double EPSILON = 1e-9;

  protected final ArkonasRanksPlugin plugin;
  @Getter
  protected final String name;
  private String value;
  @Getter
  private String sub;
  private boolean subRequirement;
  private double perRankFactor = 1.0;

  public Requirement(ArkonasRanksPlugin plugin, String name) {
    this(plugin, name, false);
  }

  public Requirement(ArkonasRanksPlugin plugin, String name, boolean subRequirement) {
    this.plugin = plugin;
    this.name = name;
    this.subRequirement = subRequirement;
  }

  protected Requirement(Requirement clone) {
    this.plugin = clone.plugin;
    this.name = clone.name;
    this.value = clone.value;
    this.sub = clone.sub;
    this.subRequirement = clone.subRequirement;
    this.perRankFactor = clone.perRankFactor;
  }

  /** Per-rank cost multiplier from the rank's {@code cost-multiplier} config key (default 1.0). */
  public void setPerRankFactor(double perRankFactor) {
    this.perRankFactor = perRankFactor;
  }

  public void setValue(String value) {
    if (hasSubRequirement()) {
      String[] parts = value.split(" ", 2);
      if (parts.length < 2) {
        throw new IllegalArgumentException("Amount and sub-requirement not present for requirement '" + getName() + "'. You must use the format '" + getName() + " <sub-requirement> <amount>'");
      }

      this.sub = parts[0];
      this.value = parts[1];
    } else {
      this.value = value;
    }
  }

  public String getValueString() {
    return value;
  }

  public String[] getValuesString() {
    return value.split(" ");
  }

  public double getValueDouble() {
    return Double.parseDouble(value);
  }

  public int getValueInt() {
    return Integer.parseInt(value);
  }

  public boolean getValueBoolean() {
    return Boolean.parseBoolean(value);
  }

  public String getFullName() {
    if (hasSubRequirement()) {
      return name + "#" + sub;
    } else {
      return name;
    }
  }

  /**
   * Check if a player meets this requirement
   *
   * @param player the player to check
   * @return true if they meet the requirement, false otherwise
   */
  public abstract boolean check(Player player);

  /**
   * Get the remaining amount needed for <code>Requirement#check(Player)</code> to yield true.
   * This is not required and is only used in placeholders.
   *
   * @param player the player to find the remaining amount of
   * @return the remaining amount needed. Should be non-negative.
   */
  public double getRemaining(Player player) {
    return check(player) ? 0 : 1;
  }

  public final boolean hasSubRequirement() {
    return subRequirement;
  }

  /**
   * The player's current cost multiplier (VIP discount, event booster, scheduled sale), or 1.0 when
   * no multiplier service is available. Currency requirements scale their cost by this so discounts
   * apply uniformly across money, XP, tokens and vote points.
   */
  protected double costFactor(Player player) {
    double global = plugin.getMultipliers() == null ? 1.0 : plugin.getMultipliers().costFactor(player);
    return global * perRankFactor;
  }

  public abstract Requirement clone();

  public double getTotal(Player player) {
    return 1;
  }

  /** Requirement names already warned about, so a broken hook logs once, not every tick. */
  private static final java.util.Set<String> WARNED_HOOK_FAILURES =
      java.util.concurrent.ConcurrentHashMap.newKeySet();

  /**
   * Records that this requirement's plugin-hook evaluation threw and was treated as unmet
   * (fail-closed), logging once per requirement name so a missing/faulty dependency degrades
   * gracefully during {@code /rankup}, menu renders and placeholder lookups instead of spamming
   * the console or surfacing as an error every tick.
   */
  protected void logHookFailureOnce(Throwable t) {
    // dedup on getFullName(): getName() is a constant per requirement TYPE (e.g. "placeholder",
    // "mcmmo"), so keying on it would silence every hook failure after the first of that type
    if (WARNED_HOOK_FAILURES.add(getFullName())) {
      plugin.getLogger().warning("Requirement '" + getFullName() + "' failed to evaluate and is being"
          + " treated as unmet; is its plugin installed and fully loaded? (" + t + ")");
    }
  }
}
