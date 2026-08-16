package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.ConfirmScreen;
import com.arkonas.ranks.menu.screens.RankupMenu;

public class RequirementStateTest extends RankupTest {

  public RequirementStateTest() {
    super("menus");
  }

  private RankupMenu openRankup(PlayerMock player) {
    player.addAttachment(plugin, "rankup.rankup", true);
    plugin.getMenuModule().openRankup(player);
    return (RankupMenu) player.getOpenInventory().getTopInventory().getHolder();
  }

  @Test
  public void unmetShowsMoneyIconUnmet() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 0);

    RankupMenu menu = openRankup(player);

    assertEquals(ConfirmScreen.State.UNMET, menu.getState());
    assertTrue(hasMaterial(menu.getInventory(), Material.GOLD_INGOT),
        "the money requirement should be shown as a GOLD_INGOT");
    assertTrue(menu.getConfirmSlot() < 0, "there must be no confirm button while unmet");
  }

  @Test
  public void unmetShowsALockedButtonThatExplainsItself() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 0);

    RankupMenu menu = openRankup(player);

    assertEquals(ConfirmScreen.State.UNMET, menu.getState());
    ItemStack locked = menu.getInventory().getItem(menu.getActionSlot());
    assertEquals(Material.RED_CONCRETE, locked.getType(),
        "the action button should be there but locked");

    String lore = plain(locked.getItemMeta().lore());
    assertTrue(lore.contains("Money"),
        "the locked lore should name the requirement that is missing, got: " + lore);
    assertTrue(lore.contains("100"),
        "the locked lore should carry the missing amount, got: " + lore);
  }

  @Test
  public void clickingTheLockedButtonDoesNotRankUp() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 0);

    RankupMenu menu = openRankup(player);
    assertEquals(ConfirmScreen.State.UNMET, menu.getState());

    player.simulateInventoryClick(menu.getActionSlot());
    server.getScheduler().performTicks(2);

    assertFalse(plugin.getPermissions().inGroup(player.getUniqueId(), "B"),
        "a click on the locked button must not rank the player up");
    assertTrue(plugin.getMenuModule().getOpenMenus().contains(menu),
        "a refused click should leave the menu open");
  }

  private static String plain(List<Component> lore) {
    if (lore == null) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    for (Component line : lore) {
      text.append(PlainTextComponentSerializer.plainText().serialize(line)).append('\n');
    }
    return text.toString();
  }

  @Test
  public void metShowsConfirmButton() {
    PlayerMock player = server.addPlayer();
    groupProvider.transferGroup(player.getUniqueId(), null, "A");
    plugin.getEconomy().setPlayer(player, 500);

    RankupMenu menu = openRankup(player);

    assertEquals(ConfirmScreen.State.READY, menu.getState());
    ItemStack confirm = menu.getInventory().getItem(menu.getConfirmSlot());
    assertEquals(Material.LIME_CONCRETE, confirm.getType(), "expected the confirm button");
  }

  private static boolean hasMaterial(Inventory inventory, Material material) {
    for (ItemStack item : inventory.getContents()) {
      if (item != null && item.getType() == material) {
        return true;
      }
    }
    return false;
  }
}
