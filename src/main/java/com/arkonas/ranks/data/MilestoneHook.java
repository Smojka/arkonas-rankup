package com.arkonas.ranks.data;

/**
 * Notified on the stats thread immediately after a rankup/prestige has been recorded, with the
 * player's up-to-date totals. Lets the milestone-reward feature react to a cumulative count without
 * racing the async write. Implementations must hop back to the main thread for any Bukkit calls.
 */
@FunctionalInterface
public interface MilestoneHook {
  void onRecord(RankupRecord record, int rankupCount, int prestigeCount);
}
