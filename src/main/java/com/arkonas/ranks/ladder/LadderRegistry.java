package com.arkonas.ranks.ladder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.arkonas.ranks.ranks.Rankups;

/**
 * Holds the server's rankup ladders by id. The ladder built from {@code rankups.yml} is always
 * present under {@link #DEFAULT}; additional ladders are loaded from files in the {@code ladders/}
 * folder, keyed by file name. A player progresses on each ladder independently, since a rank is a
 * permission group and each ladder scans only its own groups.
 *
 * <p>Ids are matched case-insensitively. Insertion order is preserved, with the default ladder
 * first, so iteration and tab-completion are stable.</p>
 */
public final class LadderRegistry {

  public static final String DEFAULT = "default";

  private final Map<String, Rankups> ladders = new LinkedHashMap<>();

  public void put(String id, Rankups rankups) {
    ladders.put(id.toLowerCase(Locale.ROOT), rankups);
  }

  /** The ladder for an id, or the default ladder when {@code id} is null; null if unknown. */
  public Rankups get(String id) {
    if (id == null) {
      return getDefault();
    }
    return ladders.get(id.toLowerCase(Locale.ROOT));
  }

  public Rankups getDefault() {
    return ladders.get(DEFAULT);
  }

  public boolean has(String id) {
    return id != null && ladders.containsKey(id.toLowerCase(Locale.ROOT));
  }

  /** All ladder ids in insertion order (default first). */
  public Set<String> ids() {
    return Collections.unmodifiableSet(ladders.keySet());
  }

  /** All ladders in insertion order (default first). */
  public Collection<Rankups> all() {
    return Collections.unmodifiableCollection(ladders.values());
  }

  public int size() {
    return ladders.size();
  }

  /** True when more than just the default ladder is configured. */
  public boolean hasMultiple() {
    return ladders.size() > 1;
  }
}
