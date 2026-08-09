package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.LadderMenu;
import com.arkonas.ranks.menu.screens.PrestigeListMenu;
import com.arkonas.ranks.menu.screens.RankPathMenu;
import com.arkonas.ranks.menu.screens.RankupMenu;

/**
 * Hand-written per-rank {@code lore:} from rankups.yml/prestiges.yml replaces the
 * generated lore, with {@code {requirements}} / {@code {rewards}} splicing the
 * generated blocks back in and the {@code lore-<variant>} keys overriding the base
 * on a single screen.
 */
public class RankLoreTest extends RankupTest {

  public RankLoreTest() {
    super("menuslore");
  }

  private static String plain(Component component) {
    return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
  }

  private static List<String> lore(ItemStack item) {
    assertNotNull(item, "expected an item to read lore from");
    List<Component> lore = item.getItemMeta().lore();
    List<String> lines = new ArrayList<>();
    if (lore != null) {
      for (Component line : lore) {
        lines.add(plain(line));
      }
    }
    return lines;
  }

  /** The first item of {@code material} in the inventory, ignoring the border panes. */
  private static ItemStack firstOfType(Inventory inventory, Material material) {
    for (ItemStack item : inventory.getContents()) {
      if (item != null && item.getType() == material) {
        return item;
      }
    }
    return null;
  }

  private PlayerMock addPlayer(String group) {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.ranks", true);
    player.addAttachment(plugin, "rankup.rankup", true);
    player.addAttachment(plugin, "rankup.prestiges", true);
    groupProvider.transferGroup(player.getUniqueId(), null, group);
    plugin.getEconomy().setPlayer(player, 0);
    return player;
  }

  private PlayerMock playerInB() {
    return addPlayer("B");
  }

  private RankPathMenu openPath(PlayerMock player) {
    plugin.getMenuModule().openRankPath(player);
    return (RankPathMenu) player.getOpenInventory().getTopInventory().getHolder();
  }

  @Test
  public void handwrittenLoreReplacesGeneratedLoreOnTheCurrentRank() {
    PlayerMock player = playerInB();
    RankPathMenu menu = openPath(player);

    int slot = menu.getCurrentSlot();
    assertTrue(slot >= 0, "the current rank should be placed");
    List<String> lines = lore(menu.getInventory().getItem(slot));

    assertEquals(5, lines.size(), "the manual lore drives the line count, got: " + lines);
    assertEquals("", lines.get(0), "a leading '' should survive as a blank spacer line");
    assertEquals("HANDWRITTEN", lines.get(1));
    assertTrue(lines.get(2).contains("100"),
        "{requirements} should expand in place, got: " + lines.get(2));
    assertEquals("next is C", lines.get(3));
    assertEquals("", lines.get(4), "a trailing '' should survive as a blank spacer line");
    assertFalse(String.join("\n", lines).contains("Click to rankup"),
        "manual lore replaces the generated click hint, got: " + lines);
  }

  @Test
  public void lockedVariantOverridesTheBaseLore() {
    PlayerMock player = playerInB();
    RankPathMenu menu = openPath(player);

    // rank C is the only locked entry; rank A is completed
    List<String> lines = lore(firstOfType(menu.getInventory(), Material.RED_STAINED_GLASS_PANE));

    assertEquals(List.of("locked handwritten"), lines);
  }

  @Test
  public void completeVariantAppliesToPassedRanks() {
    PlayerMock player = playerInB();
    RankPathMenu menu = openPath(player);

    List<String> lines = lore(firstOfType(menu.getInventory(), Material.LIME_STAINED_GLASS_PANE));

    assertEquals(List.of("completed line"), lines);
  }

  @Test
  public void infoVariantReplacesTheInfoPanelLoreAndExpandsRewards() {
    PlayerMock player = playerInB();
    plugin.getMenuModule().openRankup(player);
    RankupMenu menu = (RankupMenu) player.getOpenInventory().getTopInventory().getHolder();

    List<String> lines = lore(menu.getInventory().getItem(13));

    assertEquals(2, lines.size(), "the manual info lore drives the line count, got: " + lines);
    assertEquals("info panel line", lines.get(0));
    assertTrue(lines.get(1).contains("custom reward"),
        "{rewards} should expand into the manual lore, got: " + lines.get(1));
  }

  @Test
  public void prestigesYmlLoreDrivesThePrestigeList() {
    PlayerMock player = playerInB();
    plugin.getMenuModule().openPrestigeList(player);
    LadderMenu<?> menu =
        (PrestigeListMenu) player.getOpenInventory().getTopInventory().getHolder();

    int slot = menu.getCurrentSlot();
    assertTrue(slot >= 0, "the current prestige should be placed");

    assertEquals(List.of("prestige handwritten"), lore(menu.getInventory().getItem(slot)));
  }

  @Test
  public void ranksWithoutManualLoreKeepTheGeneratedLore() {
    PlayerMock player = addPlayer("A");

    RankPathMenu menu = openPath(player);
    List<String> lines = lore(menu.getInventory().getItem(menu.getCurrentSlot()));

    // rank A only declares lore-complete, so the current-rank item is still generated
    assertTrue(String.join("\n", lines).contains("Click to rankup"),
        "the generated lore should survive when the variant does not apply, got: " + lines);
  }
}
