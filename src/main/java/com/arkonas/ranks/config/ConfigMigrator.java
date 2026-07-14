package com.arkonas.ranks.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Brings an existing user config up to date with the shipped defaults.
 *
 * <p>Bukkit's {@code saveResource(name, false)} only writes a config file when it is absent, so a
 * server that has been running since before a feature existed never receives that feature's keys —
 * the options are simply missing and the feature stays silently off. This merges in every key the
 * defaults have and the user's file lacks, while never touching a value the user already set and
 * never removing keys it does not recognise.
 *
 * <p>The merge is pure (it works on {@link ConfigurationSection}s, not files) so it is unit-tested
 * without a server.
 */
public final class ConfigMigrator {

  private ConfigMigrator() {
  }

  /** @see #merge(ConfigurationSection, ConfigurationSection, Map) */
  public static List<String> merge(ConfigurationSection user, ConfigurationSection defaults) {
    return merge(user, defaults, Map.of());
  }

  /**
   * Copies every path present in {@code defaults} but missing from {@code user}, together with the
   * default's comments, so a newly-added option arrives documented exactly as it is in the shipped
   * config.
   *
   * <p>Migration must be <b>behaviour-preserving</b>: adding an option to a running server may not
   * change what that server already does. Most options are safe because an absent key and the
   * shipped default mean the same thing, but where they do not — {@code menus.enabled} ships as
   * {@code true} while the code reads an absent key as {@code false} — the shipped default would
   * silently switch a live server onto a different UI on a mere jar update. {@code onUpgrade} maps
   * those paths to the value that keeps the current behaviour; the option still arrives, documented
   * and ready to be turned on deliberately.
   *
   * @param onUpgrade path -> value to use instead of the shipped default when the path is missing
   * @return the paths that were added, in the order they were added (empty = already up to date)
   */
  public static List<String> merge(ConfigurationSection user, ConfigurationSection defaults,
      Map<String, Object> onUpgrade) {
    List<String> added = new ArrayList<>();
    // getKeys(true) is depth-first and yields a parent before its children, so creating a missing
    // section here always precedes setting the leaves inside it
    for (String path : defaults.getKeys(true)) {
      if (user.contains(path, true)) {
        continue; // the user has set this (even to an empty section) — leave it exactly as-is
      }

      Object value = onUpgrade.containsKey(path) ? onUpgrade.get(path) : defaults.get(path);
      if (value instanceof ConfigurationSection) {
        user.createSection(path);
      } else {
        user.set(path, value);
      }

      user.setComments(path, defaults.getComments(path));
      user.setInlineComments(path, defaults.getInlineComments(path));
      added.add(path);
    }
    return added;
  }

  /** The distinct top-level sections touched by {@link #merge}, for a readable log line. */
  public static Set<String> roots(List<String> paths) {
    Set<String> roots = new LinkedHashSet<>();
    for (String path : paths) {
      int dot = path.indexOf('.');
      roots.add(dot < 0 ? path : path.substring(0, dot));
    }
    return roots;
  }
}
