package com.arkonas.ranks.providers;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.arkonas.ranks.hook.GroupProvider;

import java.util.UUID;

public class TestGroupProvider implements GroupProvider {
  private Multimap<UUID, String> groups = ArrayListMultimap.create();

  @Override
  public boolean inGroup(UUID uuid, String group) {
    return groups.containsEntry(uuid, group.toLowerCase());
  }

  @Override
  public void transferGroup(UUID uuid, String oldGroup, String group) {
    if (oldGroup != null) {
      groups.remove(uuid, oldGroup.toLowerCase());
    }
    groups.put(uuid, group.toLowerCase());
  }
}
