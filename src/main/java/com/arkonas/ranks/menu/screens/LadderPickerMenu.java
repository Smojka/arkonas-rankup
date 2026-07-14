package com.arkonas.ranks.menu.screens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import com.arkonas.ranks.ladder.LadderRegistry;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.menu.MenuText;
import com.arkonas.ranks.ranks.Rankups;

/**
 * Lists the server's rankup ladders so a player on a multi-ladder server can pick which ladder's
 * path to view. Only shown when more than one ladder is configured; single-ladder servers open the
 * rank path directly and never see this screen.
 */
public class LadderPickerMenu extends AbstractMenu {

  private final Map<Integer, String> slotToLadder = new HashMap<>();

  public LadderPickerMenu(MenuModule module, Player player, AbstractMenu parent) {
    super(module, player, parent, module.getConfig().rows("ladders", 5));
  }

  @Override
  protected Component title() {
    return text.title(player, text.raw("ladders.title", "Ladders"), null, null);
  }

  @Override
  protected void populate() {
    slotToLadder.clear();
    LadderRegistry ladders = plugin.getLadders();
    if (ladders != null) {
      List<Integer> grid = interiorSlots();
      int i = 0;
      for (String id : ladders.ids()) {
        if (i >= grid.size()) {
          break; // more ladders than interior slots (very rare); the rest are unreachable here
        }
        int slot = grid.get(i++);
        setItem(slot, ladderButton(id));
        slotToLadder.put(slot, id);
      }
    }
    placeNav(true, true, true);
  }

  /** The 7-wide interior band (rows 1..rows-2, cols 1..7), never the border or the nav row. */
  private List<Integer> interiorSlots() {
    int rows = size() / 9;
    List<Integer> slots = new ArrayList<>();
    for (int r = 1; r <= rows - 2; r++) {
      for (int c = 1; c <= 7; c++) {
        slots.add(r * 9 + c);
      }
    }
    return slots;
  }

  private ItemStack ladderButton(String id) {
    Component name = text.component(player,
        MenuText.sub(text.raw("ladders.button.name", "&b{ladder}"), Map.of("ladder", id)));
    List<Component> lore = text.lore(player,
        text.raw("ladders.button.lore", "&7Click to view this ladder"), null, null);
    return icon(Material.CHEST, name, lore, false);
  }

  @Override
  protected void handleClick(int slot, ClickType click) {
    String id = slotToLadder.get(slot);
    if (id == null) {
      return;
    }
    Rankups ladder = plugin.getLadders() == null ? null : plugin.getLadders().get(id);
    if (ladder == null) {
      return;
    }
    theme.playSound(player, "click");
    defer(() -> new RankPathMenu(module, player, this, ladder).open());
  }
}
