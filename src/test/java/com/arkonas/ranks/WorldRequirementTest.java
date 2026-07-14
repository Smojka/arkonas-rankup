package com.arkonas.ranks;


import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import com.arkonas.ranks.placeholders.RankupExpansion;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorldRequirementTest extends RankupTest {

  public WorldRequirementTest() {
    super("world");
  }

  @Test
  public void testStatusComplete() {
    server.addSimpleWorld("world");
    server.addSimpleWorld("the_nether");

    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "a");

    RankupExpansion expansion = plugin.getPlaceholders().getExpansion();
    assertEquals("0", expansion.placeholder(player, "requirement_world_percent_done"));

    player.teleport(new Location(server.getWorld("the_nether"), 0, 0, 0));

    assertEquals("100", expansion.placeholder(player, "requirement_world_percent_done"));
  }

  @Test
  public void testMultiWorldMatchesAnyListed() {
    server.addSimpleWorld("world");
    server.addSimpleWorld("the_nether");
    server.addSimpleWorld("the_end");

    PlayerMock player = server.addPlayer();
    // rank b requires `world the_end the_nether` — the_nether is the SECOND world listed
    groupProvider.transferGroup(player.getUniqueId(), null, "b");

    RankupExpansion expansion = plugin.getPlaceholders().getExpansion();

    // pre-fix, only the first world (the_end) was ever checked, so a player in the_nether was rejected
    player.teleport(new Location(server.getWorld("the_nether"), 0, 0, 0));
    assertEquals("100", expansion.placeholder(player, "requirement_world_percent_done"));

    // a world not in the list still fails
    player.teleport(new Location(server.getWorld("world"), 0, 0, 0));
    assertEquals("0", expansion.placeholder(player, "requirement_world_percent_done"));
  }
}
