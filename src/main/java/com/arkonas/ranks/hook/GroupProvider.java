package com.arkonas.ranks.hook;

import java.util.UUID;

public interface GroupProvider {
  boolean inGroup(UUID uuid, String group);
  void transferGroup(UUID uuid, String oldGroup, String group);

  /**
   * Removes a group from the player without granting another. Unlike
   * {@link #transferGroup(UUID, String, String)} (whose target must be non-null), this supports a
   * pure removal — used to reset prestige on a rebirth. No-op by default.
   */
  default void removeGroup(UUID uuid, String group) {
  }
}
