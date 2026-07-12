package com.arkonas.ranks.commands;

import com.arkonas.ranks.RankupTest;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Regression test for the {@code /aru forceprestige} NPE: {@code prestiges.getByPlayer} returns null
 * for a player who has not reached the top rank (the normal case an admin forces a prestige on), and
 * the command dereferenced it without a null check. It should now message gracefully.
 */
public class ForcePrestigeTest extends RankupTest {

  public ForcePrestigeTest() {
    super("prestigerequirements"); // prestige enabled; first prestige is from rank B
  }

  @Test
  public void forcePrestigeOnNonTopRankPlayerMessagesInsteadOfThrowing() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.force", true);
    // player is in rank A, not the top rank B, so they are in no prestige 'from' group
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getCommand("rankup3").execute(player, "pru",
        new String[] {"forceprestige", player.getName()});

    // pre-fix this path NPE'd before sending anything
    player.assertSaid(ChatColor.YELLOW + "That player is not in any prestige groups.");
  }
}
