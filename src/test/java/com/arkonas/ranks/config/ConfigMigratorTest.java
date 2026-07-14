package com.arkonas.ranks.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * An older config (one written before a feature shipped) must gain the new options without losing or
 * changing anything the server owner already set.
 */
public class ConfigMigratorTest {

  private YamlConfiguration yaml(String source) {
    return YamlConfiguration.loadConfiguration(new StringReader(source));
  }

  private YamlConfiguration defaults() {
    return yaml("""
        version: 11
        prestige: false
        # documented in the shipped file
        progress-display:
          enabled: false
          bossbar:
            enabled: false
            text: '&aNext: %next%'
        boosters:
          schedule: []
        """);
  }

  @Test
  public void addsMissingSectionsAndKeepsUserValues() {
    YamlConfiguration user = yaml("""
        version: 10
        prestige: true
        """);

    List<String> added = ConfigMigrator.merge(user, defaults());

    // the user's own values survive untouched
    assertTrue(user.getBoolean("prestige"), "an existing value must never be overwritten");
    assertEquals(10, user.getInt("version"), "merge does not stamp the version itself");

    // and every missing option arrives, nested ones included
    assertTrue(user.contains("progress-display.bossbar.enabled", true));
    assertEquals("&aNext: %next%", user.getString("progress-display.bossbar.text"));
    assertTrue(user.contains("boosters.schedule", true));
    assertTrue(added.contains("progress-display"), () -> "added: " + added);
    assertEquals(java.util.Set.of("progress-display", "boosters"), ConfigMigrator.roots(added));
  }

  @Test
  public void migrationIsBehaviourPreservingForOptionsWhoseAbsenceMeansOff() {
    // menus.enabled ships as `true`, but the code reads an absent key as `false`. Adopting the
    // shipped default on upgrade would silently move a live server onto a different UI, so the
    // option must arrive switched off.
    YamlConfiguration defaults = yaml("""
        menus:
          enabled: true
        """);
    YamlConfiguration user = yaml("prestige: true\n");

    List<String> added = ConfigMigrator.merge(user, defaults,
        java.util.Map.of("menus.enabled", false));

    assertTrue(added.contains("menus.enabled"), "the option must still be added + documented");
    assertFalse(user.getBoolean("menus.enabled"),
        "but switched off, so a jar update does not change what the server already does");
  }

  @Test
  public void keepsUnknownUserKeysAndIsIdempotent() {
    YamlConfiguration user = yaml("""
        version: 10
        my-custom-key: keep me
        progress-display:
          enabled: true
        """);

    ConfigMigrator.merge(user, defaults());

    // an option the defaults do not know about is not pruned
    assertEquals("keep me", user.getString("my-custom-key"));
    // a value the user already set inside a partially-present section stays as they set it...
    assertTrue(user.getBoolean("progress-display.enabled"));
    // ...while the missing siblings are filled in
    assertFalse(user.getBoolean("progress-display.bossbar.enabled"));

    // running it again is a no-op: nothing left to add
    assertTrue(ConfigMigrator.merge(user, defaults()).isEmpty(), "migration must be idempotent");
  }
}
