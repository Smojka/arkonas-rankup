package com.arkonas.ranks.toml;

import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.ranks.Rankups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TomlTest extends RankupTest {

  public TomlTest() {
    super("toml");
  }

  @Test
  public void testRequirementsNotMet() {
    PlayerMock player = server.addPlayer();

    Rankups ranks = plugin.getRankups();
    assertEquals(1500, ranks.getFirst().getRequirement(null, "money").getValueDouble());

    plugin.getPermissions().transferGroup(player.getUniqueId(), null, "C");
    player.addAttachment(plugin, "rankup.rankup", true);
    plugin.getHelper().rankup(player);

    player.assertSaid("toml");
  }

  @Test
  public void perRankCelebrationOverrideSurvivesTomlDeserialization() {
    org.bukkit.configuration.ConfigurationSection celebration =
        plugin.getRankups().getRankByName("A").getSection().getConfigurationSection("celebration");
    assertNotNull(celebration, "TOML per-rank celebration: override should be retained");
    assertEquals("block.note_block.pling",
        celebration.getConfigurationSection("sound").getString("name"),
        "the nested celebration value should keep its structure through TOML flatten + rebuild");
  }

  @Test
  public void testRankup() {
    PlayerMock player = server.addPlayer();
    plugin.getPermissions().transferGroup(player.getUniqueId(), null, "B");
    plugin.getEconomy().setPlayer(player, 10000);
    player.addAttachment(plugin, "rankup.rankup", true);

    plugin.getHelper().rankup(player);

    assertTrue(plugin.getPermissions().inGroup(player.getUniqueId(), "C"));
  }

  @Test
  public void testRanks() {
    PlayerMock player = server.addPlayer();
    plugin.getPermissions().transferGroup(player.getUniqueId(), null, "C");

    player.addAttachment(plugin, "rankup.ranks", true);
    plugin.getCommand("ranks").execute(player, "ranks", new String[0]);
    player.assertSaid(ChatColor.GRAY + "A " + ChatColor.DARK_GRAY + "\u00bb " + ChatColor.GRAY + "B");
    player.assertSaid(ChatColor.GRAY + "B " + ChatColor.DARK_GRAY + "\u00bb " + ChatColor.GRAY + "C");
    player.assertSaid(ChatColor.RED + "C " + ChatColor.YELLOW + "\u00bb " + ChatColor.RED + "D o");
    player.assertNoMoreSaid();
  }
}
