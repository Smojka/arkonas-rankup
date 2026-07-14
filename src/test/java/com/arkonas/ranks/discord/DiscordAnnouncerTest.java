package com.arkonas.ranks.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Pure announcement selection/rendering (no Bukkit event plumbing, no live DiscordSRV). */
class DiscordAnnouncerTest {

  /** Records every dispatched (channel, message) so tests can assert delivery. */
  private static final class CapturingSender implements DiscordSender {
    final List<String[]> sent = new ArrayList<>();

    @Override
    public void send(String channel, String message) {
      sent.add(new String[]{channel, message});
    }
  }

  // --- render ---------------------------------------------------------------------------------

  @Test
  void renderSubstitutesAllPlaceholders() {
    assertEquals("Steve: A -> B (rankup)",
        DiscordAnnouncer.render("%player%: %from% -> %to% (%type%)", "Steve", "A", "B", "rankup"));
  }

  @Test
  void renderToleratesNulls() {
    assertEquals(":  ->  (rankup)",
        DiscordAnnouncer.render("%player%: %from% -> %to% (%type%)", null, null, null, "rankup"));
  }

  // --- dispatch -------------------------------------------------------------------------------

  @Test
  void rankupSendsRenderedMessageToChannel() {
    CapturingSender sender = new CapturingSender();
    DiscordAnnouncer announcer = new DiscordAnnouncer(true, "ranks",
        "**%player%** ranked up to **%to%**", "", sender);

    announcer.announceRankup("Steve", "A", "B");

    assertEquals(1, sender.sent.size());
    assertEquals("ranks", sender.sent.get(0)[0]);
    assertEquals("**Steve** ranked up to **B**", sender.sent.get(0)[1]);
  }

  @Test
  void prestigeUsesPrestigeTemplateAndType() {
    CapturingSender sender = new CapturingSender();
    DiscordAnnouncer announcer = new DiscordAnnouncer(true, "global",
        "rankup", "%player% prestiged (%type%)", sender);

    announcer.announcePrestige("Alex", "P1", "P2");

    assertEquals(1, sender.sent.size());
    assertEquals("Alex prestiged (prestige)", sender.sent.get(0)[1]);
  }

  @Test
  void blankTemplateSendsNothing() {
    CapturingSender sender = new CapturingSender();
    DiscordAnnouncer announcer = new DiscordAnnouncer(true, "global", "  ", "", sender);
    announcer.announceRankup("Steve", "A", "B");
    assertTrue(sender.sent.isEmpty());
  }

  @Test
  void disabledSendsNothing() {
    CapturingSender sender = new CapturingSender();
    DiscordAnnouncer announcer = new DiscordAnnouncer(false, "global", "%player%", "", sender);
    assertFalse(announcer.isEnabled());
    announcer.announceRankup("Steve", "A", "B");
    assertTrue(sender.sent.isEmpty());
  }

  @Test
  void nullSenderIsInertNotFatal() {
    DiscordAnnouncer announcer = new DiscordAnnouncer(true, "global", "%player%", "%player%", null);
    assertFalse(announcer.isEnabled());
    announcer.announceRankup("Steve", "A", "B"); // must not throw
    announcer.announcePrestige("Steve", "A", "B");
  }

  // --- fromConfig -----------------------------------------------------------------------------

  @Test
  void parsesConfig() {
    CapturingSender sender = new CapturingSender();
    YamlConfiguration config = new YamlConfiguration();
    config.set("enabled", true);
    config.set("channel", "ranks");
    config.set("rankup-message", "%player% -> %to%");
    config.set("prestige-message", "%player% prestige");

    DiscordAnnouncer announcer = DiscordAnnouncer.fromConfig(config, sender);
    assertTrue(announcer.isEnabled());
    announcer.announceRankup("Steve", "A", "B");
    assertEquals("ranks", sender.sent.get(0)[0]);
    assertEquals("Steve -> B", sender.sent.get(0)[1]);
  }

  @Test
  void disabledConfigIsInert() {
    DiscordAnnouncer announcer = DiscordAnnouncer.fromConfig(null, new CapturingSender());
    assertFalse(announcer.isEnabled());
  }
}
