package com.arkonas.ranks.effects;

import com.arkonas.ranks.RankupTest;
import org.bukkit.Sound;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Regression test for the per-rank {@code celebration:} override on the rankup ladder. The rank's
 * section was built from the {@code rankup:} messages only, dropping the {@code celebration} key, so
 * EffectsListener always fell back to the global effects.yml sound. Rank A overrides it with a
 * note-block pling; that should now win over the global level-up sound.
 */
public class PerRankCelebrationTest extends RankupTest {

  public PerRankCelebrationTest() {
    super("celebration");
  }

  @Test
  public void perRankCelebrationOverridesGlobalSound() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 10000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getHelper().rankup(player);

    player.assertSoundHeard(Sound.BLOCK_NOTE_BLOCK_PLING);
  }
}
