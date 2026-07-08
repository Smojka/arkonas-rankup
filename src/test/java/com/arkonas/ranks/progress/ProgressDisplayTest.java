package com.arkonas.ranks.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.arkonas.ranks.RankupTest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Live progress display over the bundled A -> B (money 1000) ladder: the completion fraction and
 * the experience-bar mirror.
 */
public class ProgressDisplayTest extends RankupTest {

  private ProgressDisplay display(boolean expBar) {
    YamlConfiguration config = new YamlConfiguration();
    config.set("enabled", true);
    config.set("expbar", expBar);
    return ProgressDisplay.fromConfig(plugin, config);
  }

  @Test
  public void fractionIsRequirementCompletion() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 500); // half of the 1000 needed for A -> B
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    assertEquals(0.5, display(false).fractionToNext(player), 1e-6);
  }

  @Test
  public void fractionIsFullWhenTopOrNotInLadder() {
    ProgressDisplay display = display(false);

    PlayerMock notInLadder = server.addPlayer();
    assertEquals(1.0, display.fractionToNext(notInLadder), 1e-6);

    PlayerMock top = server.addPlayer();
    groupProvider.transferGroup(top.getUniqueId(), null, "D"); // final group
    assertEquals(1.0, display.fractionToNext(top), 1e-6);
  }

  @Test
  public void expBarMirrorsProgress() {
    PlayerMock player = server.addPlayer();
    plugin.getEconomy().setPlayer(player, 500);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    display(true).update(player);

    assertEquals(0.5f, player.getExp(), 1e-4f);
  }

  @Test
  public void disabledConfigYieldsNoDisplay() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("enabled", false);
    assertEquals(null, ProgressDisplay.fromConfig(plugin, config));
  }
}
