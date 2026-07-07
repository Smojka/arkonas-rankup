package com.arkonas.ranks.effects;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;

public class EffectsTest extends RankupTest {

  @Test
  public void testCelebrationSoundPlayedOnRankup() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 10000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getHelper().rankup(player);

    // default effects.yml plays entity.player.levelup on rankup
    player.assertSoundHeard(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP);
  }

  @Test
  public void testEffectsConfigGenerated() {
    assertNotNull(plugin.getEffectsListener());
  }
}
