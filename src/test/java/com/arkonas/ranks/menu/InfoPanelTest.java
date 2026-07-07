package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.RankupMenu;

/**
 * The rank-info panel (slot 13) names the rank being advanced to and lists its
 * rewards, including per-rank {@code rankup.rewards} overrides.
 */
public class InfoPanelTest extends RankupTest {

  public InfoPanelTest() {
    super("menus");
  }

  private RankupMenu openRankup(PlayerMock player) {
    plugin.getMenuModule().openRankup(player);
    return (RankupMenu) player.getOpenInventory().getTopInventory().getHolder();
  }

  private static String plain(Component component) {
    return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
  }

  @Test
  public void infoPanelNamesNextRankAndListsRewards() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 0); // stay UNMET so we exercise placeRequirements()

    RankupMenu menu = openRankup(player);

    ItemStack panel = menu.getInventory().getItem(13);
    assertNotNull(panel, "the rank-info panel must be placed at slot 13");

    String name = plain(panel.getItemMeta().displayName());
    assertTrue(name.contains("Beta"),
        "the panel name should show the next rank's display-name, got: " + name);

    List<Component> lore = panel.getItemMeta().lore();
    assertNotNull(lore, "the panel must carry a rewards lore block");
    StringBuilder loreText = new StringBuilder();
    for (Component line : lore) {
      loreText.append(plain(line)).append('\n');
    }
    assertTrue(loreText.toString().toLowerCase().contains("reward"),
        "the panel lore should contain a rewards line, got: " + loreText);
  }

  @Test
  public void perRankRewardsOverrideSurfacesInPanel() {
    PlayerMock player = server.addPlayer();
    // rank A carries `rankup: { rewards: '&8• custom reward' }` in the fixture
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 500); // READY -> placeConfirm() also builds the panel

    RankupMenu menu = openRankup(player);

    ItemStack panel = menu.getInventory().getItem(13);
    assertNotNull(panel, "the rank-info panel must be placed at slot 13");

    StringBuilder loreText = new StringBuilder();
    for (Component line : panel.getItemMeta().lore()) {
      loreText.append(plain(line)).append('\n');
    }
    assertTrue(loreText.toString().contains("custom reward"),
        "the per-rank rewards override should surface in the panel lore, got: " + loreText);
  }
}
