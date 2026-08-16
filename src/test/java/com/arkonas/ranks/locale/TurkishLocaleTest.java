package com.arkonas.ranks.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.arkonas.ranks.RankupTest;
import com.arkonas.ranks.menu.screens.ConfirmScreen;
import com.arkonas.ranks.menu.screens.RankupMenu;

public class TurkishLocaleTest extends RankupTest {

  public TurkishLocaleTest() {
    super("turkish");
  }

  @Test
  public void testTurkishMenuConfirm() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.rankup", true);
    plugin.getEconomy().setPlayer(player, 1000);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getMenuModule().openRankup(player);
    RankupMenu menu = (RankupMenu) player.getOpenInventory().getTopInventory().getHolder();
    assertEquals(ConfirmScreen.State.READY, menu.getState());

    ItemStack confirm = menu.getInventory().getItem(menu.getConfirmSlot());
    String name = PlainTextComponentSerializer.plainText().serialize(confirm.displayName());
    assertTrue(name.contains("RÜTBE ATLA"), "expected Turkish confirm label, got: " + name);
  }

  @Test
  public void testTurkishLockedButton() {
    PlayerMock player = server.addPlayer();
    player.addAttachment(plugin, "rankup.rankup", true);
    plugin.getEconomy().setPlayer(player, 0);
    groupProvider.transferGroup(player.getUniqueId(), null, "A");

    plugin.getMenuModule().openRankup(player);
    RankupMenu menu = (RankupMenu) player.getOpenInventory().getTopInventory().getHolder();
    assertEquals(ConfirmScreen.State.UNMET, menu.getState());

    ItemStack locked = menu.getInventory().getItem(menu.getActionSlot());
    String name = PlainTextComponentSerializer.plainText().serialize(locked.displayName());
    assertTrue(name.contains("ŞARTLARI SAĞLAMIYORSUN"),
        "expected the Turkish locked label, got: " + name);

    StringBuilder lore = new StringBuilder();
    for (net.kyori.adventure.text.Component line : locked.getItemMeta().lore()) {
      lore.append(PlainTextComponentSerializer.plainText().serialize(line)).append('\n');
    }
    assertTrue(lore.toString().contains("Şu şartlar eksik"),
        "expected the Turkish locked lore, got: " + lore);
    assertTrue(lore.toString().contains("Para"),
        "the locked lore should name the missing requirement in Turkish, got: " + lore);
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
