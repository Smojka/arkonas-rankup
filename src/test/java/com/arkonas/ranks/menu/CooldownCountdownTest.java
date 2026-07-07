package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.ConfirmScreen;
import com.arkonas.ranks.menu.screens.RankupMenu;

/**
 * The cooldown is wall-clock based (RankupHelper), so this test uses small real
 * sleeps to let it expire while the menu ticker counts down.
 */
public class CooldownCountdownTest extends RankupTest {

  public CooldownCountdownTest() {
    super("menus"); // cooldown: 2
  }

  @Test
  public void countdownTicksDownThenReadies() throws InterruptedException {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 1000);

    // rank up once: applies the cooldown, lands the player in B (which they can
    // still afford), so the next rankup is gated only by the cooldown
    plugin.getHelper().rankup(player);
    assertTrue(plugin.getPermissions().inGroup(player.getUniqueId(), "B"));

    plugin.getMenuModule().openRankup(player);
    RankupMenu menu = (RankupMenu) player.getOpenInventory().getTopInventory().getHolder();

    assertEquals(ConfirmScreen.State.COOLDOWN, menu.getState(), "should be on cooldown");
    assertTrue(hasClock(menu), "a countdown clock item should be shown");
    long initial = menu.getShownCooldownSeconds();
    assertTrue(initial > 0, "the countdown should show remaining seconds");

    // let ~1s pass and tick: the shown seconds must decrease
    Thread.sleep(1100);
    server.getScheduler().performTicks(4);
    assertTrue(menu.getShownCooldownSeconds() < initial, "the countdown should decrease");

    // let the rest of the cooldown pass: the menu flips to READY
    Thread.sleep(1300);
    server.getScheduler().performTicks(4);
    assertEquals(ConfirmScreen.State.READY, menu.getState(),
        "once the cooldown expires the menu should show the confirm button");
  }

  private static boolean hasClock(RankupMenu menu) {
    for (org.bukkit.inventory.ItemStack item : menu.getInventory().getContents()) {
      if (item != null && item.getType() == Material.CLOCK) {
        return true;
      }
    }
    return false;
  }
}
