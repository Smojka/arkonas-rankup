package com.arkonas.ranks.effects;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.arkonas.ranks.RankupTest;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** The rankup jingle: an opt-in ascending note-block sequence scheduled on the main thread. */
public class JingleTest extends RankupTest {

  private YamlConfiguration jingleSection(boolean enabled) {
    YamlConfiguration section = new YamlConfiguration();
    section.set("jingle.enabled", enabled);
    section.set("jingle.sound", "block.note_block.pling");
    section.set("jingle.notes", 3);
    section.set("jingle.interval-ticks", 2);
    return section;
  }

  @Test
  public void jinglePlaysWhenEnabled() {
    PlayerMock player = server.addPlayer();

    new CelebrationEffects(plugin).play(player, jingleSection(true), "A", "B");
    server.getScheduler().performTicks(10); // let the scheduled notes fire

    player.assertSoundHeard(Sound.BLOCK_NOTE_BLOCK_PLING);
  }

  @Test
  public void jingleSilentWhenDisabled() {
    PlayerMock player = server.addPlayer();

    new CelebrationEffects(plugin).play(player, jingleSection(false), "A", "B");
    server.getScheduler().performTicks(10);

    assertThrows(AssertionError.class,
        () -> player.assertSoundHeard(Sound.BLOCK_NOTE_BLOCK_PLING));
  }
}
