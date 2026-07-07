package com.arkonas.ranks.data;

import java.util.UUID;

/**
 * Immutable snapshot of a single rankup/prestige action, captured on the main
 * thread and written asynchronously.
 */
public record RankupRecord(UUID uuid, String name, Type type,
                           String fromRank, String toRank, long timestamp) {

  public enum Type {
    RANKUP, PRESTIGE, RANKDOWN, FORCE_RANKUP, FORCE_PRESTIGE
  }
}
