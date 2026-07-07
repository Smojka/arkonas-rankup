package com.arkonas.ranks.requirements;

import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.ranks.Rank;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MobKillsRequirementsTest extends RankupTest {

  public MobKillsRequirementsTest() {
    super("mobkillsrequirements");
  }

  @Test
  public void testMobKillsRequirements() {
    PlayerMock player = server.addPlayer();

    player.setStatistic(Statistic.KILL_ENTITY, EntityType.SNOW_GOLEM, 2);
    player.setStatistic(Statistic.KILL_ENTITY, EntityType.MOOSHROOM, 1);

    Rank rank = plugin.getRankups().getFirst();

    assertEquals(3 - 2, rank.getRequirement(player, "mob-kills#snow_golem").getRemaining(player));
    assertEquals(3 - 1, rank.getRequirement(player, "mob-kills#mooshroom").getRemaining(player));
  }
}
