package com.arkonas.ranks.multiplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.RankupTest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Cost multipliers: the {@link MultiplierService} factor logic, plus an end-to-end check that a
 * discount reduces both the affordability threshold and the amount deducted on the bundled
 * A -> B ladder (money 1000).
 */
public class MultiplierTest extends RankupTest {

  @Test
  public void disabledIsNeutral() {
    PlayerMock player = server.addPlayer();
    assertEquals(1.0, MultiplierService.disabled().costFactor(player), 1e-9);
  }

  @Test
  public void globalFactorApplies() {
    PlayerMock player = server.addPlayer();
    assertEquals(0.5, new MultiplierService(0.5, Map.of()).costFactor(player), 1e-9);
  }

  @Test
  public void bestPermissionFactorWins() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.multiplier.vip", true);
    player.addAttachment(plugin, "rankup.multiplier.mvp", true);
    Map<String, Double> perms = new LinkedHashMap<>();
    perms.put("rankup.multiplier.vip", 0.9);
    perms.put("rankup.multiplier.mvp", 0.8);
    // global 1.0 * best (lowest) permission factor 0.8
    assertEquals(0.8, new MultiplierService(1.0, perms).costFactor(player), 1e-9);
  }

  @Test
  public void eventBoosterAppliesWhileActive() {
    PlayerMock player = server.addPlayer();
    MultiplierService service = new MultiplierService(1.0, Map.of());
    service.setEventMultiplier(0.5, Long.MAX_VALUE);
    assertEquals(0.5, service.costFactor(player), 1e-9);

    service.setEventMultiplier(0.5, 0L); // expired
    assertEquals(1.0, service.costFactor(player), 1e-9);
  }

  @Test
  public void discountReducesRankupCostAndDeduction() {
    PlayerMock player = server.addPlayer();
    // A -> B normally costs 1000; a 50% discount makes it 500
    plugin.getMultipliers().setEventMultiplier(0.5, Long.MAX_VALUE);
    plugin.getEconomy().setPlayer(player, 500);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getHelper().rankup(player);

    assertTrue(groupProvider.inGroup(player.getUniqueId(), "B"),
        "500 should afford the discounted 500 cost");
    assertEquals(0, plugin.getEconomy().getBalance(player), 0);
  }
}
