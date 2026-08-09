package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.RankPathMenu;

/**
 * The generated requirement block is driven by the {@code menu.requirements} templates:
 * per-state lines, per-requirement overrides (including multi-line ones and their own
 * number format), a header/footer, {@code hide-met} and {@code sort}.
 */
public class RequirementLoreTest extends RankupTest {

  public RequirementLoreTest() {
    super("menusreqlore");
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

  private static ItemStack firstOfType(Inventory inventory, Material material) {
    for (ItemStack item : inventory.getContents()) {
      if (item != null && item.getType() == material) {
        return item;
      }
    }
    return null;
  }

  /** A player standing on rank A: 250 money of 1,000, no playtime and no {@code rankup.vip}. */
  private PlayerMock playerInA(double money) {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.ranks", true);
    player.addAttachment(plugin, "rankup.rankup", true);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, money);
    player.setStatistic(Statistic.PLAY_ONE_MINUTE, 0);
    return player;
  }

  private List<String> currentRankLore(PlayerMock player) {
    plugin.getMenuModule().openRankPath(player);
    RankPathMenu menu = (RankPathMenu) player.getOpenInventory().getTopInventory().getHolder();
    int slot = menu.getCurrentSlot();
    assertTrue(slot >= 0, "the current rank should be placed");
    return lore(menu.getInventory().getItem(slot));
  }

  @Test
  public void everyLineComesFromTheConfiguredTemplates() {
    List<String> lines = currentRankLore(playerInA(250));

    assertEquals(List.of(
        "",
        "REQUIREMENTS",
        // line-unmet, with the money format on both sides of the slash
        "! Money: 250/1,000 (25%) NO",
        // types.playtime-minutes.line-unmet: three lines, values through time-format
        "| Playtime",
        "  have 0d 0h 0m",
        "  need 0d 1h 30m (0%)",
        // a requirement with nothing to count falls to line-flat
        "? Permission NO",
        "end of list",
        "Click to rankup"), lines);
  }

  @Test
  public void metRequirementsFallBackToTheProgressLine() {
    List<String> lines = currentRankLore(playerInA(1500));

    assertTrue(lines.contains("| Money: 1,000/1,000 (100%)"),
        "a met requirement should use line-progress and cap at the total, got: " + lines);
  }

  @Test
  public void flatRequirementsShowTheMetStatus() {
    PlayerMock player = playerInA(250);
    player.addAttachment(plugin, "rankup.vip", true);

    assertTrue(currentRankLore(player).contains("? Permission YES"));
  }

  /** A state variant beats the screen variant, so an unmet locked rank still uses line-unmet. */
  @Test
  public void lockedRanksKeepTheStateVariant() {
    List<String> lines = lockedRankLore(playerInA(250));

    assertEquals(List.of("", "REQUIREMENTS", "! Money: 250/500 (50%) NO", "end of list"), lines);
  }

  /** Without a state variant the locked rank falls back to the plain, progress-less line. */
  @Test
  public void lockedRanksFallBackToThePlainLine() {
    plugin.getMessages().set("menu.requirements.line-unmet", null);

    List<String> lines = lockedRankLore(playerInA(250));

    assertEquals(List.of("", "REQUIREMENTS", "- Money needs 500", "end of list"), lines);
  }

  /** The first red pane on the path, i.e. rank B: locked, and gated on 500 money. */
  private List<String> lockedRankLore(PlayerMock player) {
    plugin.getMenuModule().openRankPath(player);
    RankPathMenu menu = (RankPathMenu) player.getOpenInventory().getTopInventory().getHolder();
    return lore(firstOfType(menu.getInventory(), Material.RED_STAINED_GLASS_PANE));
  }

  /** Both axes at once: line-progress-unmet only applies on the rank being worked on. */
  @Test
  public void theProgressAndStateVariantCanBeCombined() {
    plugin.getMessages().set("menu.requirements.line-progress-unmet", "working on {name}");

    PlayerMock player = playerInA(250);

    assertTrue(currentRankLore(player).contains("working on Money"),
        "the current rank should use line-progress-unmet");
    assertTrue(lockedRankLore(player).contains("! Money: 250/500 (50%) NO"),
        "a locked rank should keep line-unmet");
  }

  @Test
  public void hideMetDropsFinishedRequirements() {
    plugin.getMessages().set("menu.requirements.hide-met", true);

    List<String> lines = currentRankLore(playerInA(1500));

    assertFalse(String.join("\n", lines).contains("Money"),
        "the met money requirement should be hidden, got: " + lines);
    assertTrue(lines.contains("| Playtime"), "unmet requirements stay, got: " + lines);
  }

  @Test
  public void sortPutsUnmetRequirementsFirst() {
    plugin.getMessages().set("menu.requirements.sort", "unmet-first");

    List<String> lines = currentRankLore(playerInA(1500));

    assertEquals("| Playtime", lines.get(2), "the unmet requirements come first, got: " + lines);
    assertEquals("| Money: 1,000/1,000 (100%)", lines.get(6),
        "the met one is pushed to the end of the block, got: " + lines);
  }
}
