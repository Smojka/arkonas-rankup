package com.arkonas.ranks.locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;

public class TurkishLocaleTest extends RankupTest {

  public TurkishLocaleTest() {
    super("turkish");
  }

  @Test
  public void testTurkishSuccessMessage() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 1000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getHelper().rankup(player);

    String message = player.nextMessage();
    assertTrue(message.contains("yeni rütbesine yükseldi"),
        "expected Turkish success-public, got: " + message);
  }

  @Test
  public void testTurkishRequirementsNotMet() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 0);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getHelper().rankup(player);

    String message = player.nextMessage();
    assertTrue(message.contains("paraya ihtiyacın var"),
        "expected Turkish requirements-not-met, got: " + message);
  }
}
