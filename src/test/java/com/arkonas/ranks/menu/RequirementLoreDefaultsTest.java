package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.RankPathMenu;

/**
 * With no {@code menu.requirements} section configured the block renders exactly what it
 * rendered before it became configurable, so an untouched locale file is unaffected.
 */
public class RequirementLoreDefaultsTest extends RankupTest {

  public RequirementLoreDefaultsTest() {
    super("menus");
  }

  private static List<String> lore(ItemStack item) {
    List<Component> lore = item.getItemMeta().lore();
    List<String> lines = new ArrayList<>();
    if (lore != null) {
      for (Component line : lore) {
        lines.add(PlainTextComponentSerializer.plainText().serialize(line));
      }
    }
    return lines;
  }

  @Test
  public void theBlockKeepsItsBuiltInShape() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.ranks", true);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 100);

    plugin.getMenuModule().openRankPath(player);
    RankPathMenu menu = (RankPathMenu) player.getOpenInventory().getTopInventory().getHolder();
    int slot = menu.getCurrentSlot();
    assertTrue(slot >= 0, "the current rank should be placed");

    assertEquals(List.of("• Money: 100 (100%)", "Click to rankup"),
        lore(menu.getInventory().getItem(slot)));
  }
}
