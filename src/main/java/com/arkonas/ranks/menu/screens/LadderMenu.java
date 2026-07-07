package com.arkonas.ranks.menu.screens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.menu.MenuText;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import org.bukkit.entity.Player;

/**
 * Shared base for the paginated ladder views (rank path, prestige list). Lays a
 * 7x4 grid of rank entries per page with prev/next arrows and a bottom-row
 * navigation bar, and computes each entry's completed/current/locked state.
 */
public abstract class LadderMenu<T extends Rank> extends AbstractMenu {

  public enum EntryState { COMPLETE, CURRENT, LOCKED }

  protected static final int PER_PAGE = 28;

  private final int prevSlot = 45;
  private final int backSlot = 46;
  private final int homeSlot = 48;
  private final int pageSlot = 49;
  private final int closeSlot = 52;
  private final int nextSlot = 53;

  @Getter
  private int page;
  @Getter
  private int totalPages = 1;
  @Getter
  private int placedEntries;
  @Getter
  private int currentSlot = -1;

  private final Map<Integer, Integer> slotToIndex = new HashMap<>();
  private List<RankElement<T>> entries = new ArrayList<>();

  protected LadderMenu(MenuModule module, Player player, AbstractMenu parent, int rows, int page) {
    super(module, player, parent, rows);
    this.page = page;
  }

  // --- subclass contract ----------------------------------------------------

  protected abstract String menuKey();

  protected abstract List<RankElement<T>> ladderEntries();

  protected abstract RankElement<T> currentEntry();

  protected abstract ItemStack entryItem(RankElement<T> element, EntryState state);

  protected abstract void onEntryClick(RankElement<T> element, EntryState state);

  protected abstract LadderMenu<T> pageMenu(int page);

  // --- layout ---------------------------------------------------------------

  @Override
  protected Component title() {
    return text.title(player, text.raw(menuKey() + ".title", "Ranks"), null, null);
  }

  @Override
  protected void populate() {
    this.entries = ladderEntries();
    this.slotToIndex.clear();
    this.placedEntries = 0;
    this.currentSlot = -1;

    RankElement<T> current = currentEntry();
    EntryState[] states = computeStates(entries, current);

    this.totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PER_PAGE));
    if (page >= totalPages) {
      page = totalPages - 1;
    }
    if (page < 0) {
      page = 0;
    }

    List<Integer> grid = innerGrid();
    for (int i = 0; i < PER_PAGE && i < grid.size(); i++) {
      int index = page * PER_PAGE + i;
      if (index >= entries.size()) {
        break;
      }
      int slot = grid.get(i);
      RankElement<T> element = entries.get(index);
      setItem(slot, entryItem(element, states[index]));
      slotToIndex.put(slot, index);
      placedEntries++;
      if (states[index] == EntryState.CURRENT) {
        currentSlot = slot;
      }
    }

    // pagination + navigation (bottom row, manually laid out so it never
    // collides with the 45/53 page arrows)
    if (page > 0) {
      setItem(prevSlot, navItem(Material.SPECTRAL_ARROW,
          text.raw("common.prev-page", "&a« Previous"), theme.primary()));
    }
    if (page < totalPages - 1) {
      setItem(nextSlot, navItem(Material.SPECTRAL_ARROW,
          text.raw("common.next-page", "&aNext »"), theme.primary()));
    }
    setItem(pageSlot, navItem(Material.PAPER,
        MenuText.sub(text.raw("common.page", "&7Page {page}/{pages}"),
            Map.of("page", String.valueOf(page + 1), "pages", String.valueOf(totalPages))),
        theme.muted()));
    if (getParent() != null) {
      setItem(backSlot, navItem(Material.ARROW, text.raw("common.back", "&7« Back"), theme.muted()));
    }
    setItem(homeSlot, navItem(Material.NETHER_STAR, text.raw("common.home", "&b☰ Menu"),
        theme.primary()));
    setItem(closeSlot, navItem(Material.BARRIER, text.raw("common.close", "&cClose"),
        theme.danger()));
  }

  @Override
  protected void handleClick(int slot, ClickType click) {
    Integer index = slotToIndex.get(slot);
    if (index != null) {
      EntryState state = computeStates(entries, currentEntry())[index];
      onEntryClick(entries.get(index), state);
      return;
    }
    if (slot == prevSlot && page > 0) {
      theme.playSound(player, "page");
      defer(() -> pageMenu(page - 1).open());
    } else if (slot == nextSlot && page < totalPages - 1) {
      theme.playSound(player, "page");
      defer(() -> pageMenu(page + 1).open());
    } else if (slot == homeSlot) {
      theme.playSound(player, "click");
      defer(() -> module.openHub(player));
    } else if (slot == backSlot && getParent() != null) {
      theme.playSound(player, "click");
      defer(() -> getParent().open());
    } else if (slot == closeSlot) {
      theme.playSound(player, "click");
      defer(player::closeInventory);
    }
  }

  private EntryState[] computeStates(List<RankElement<T>> list, RankElement<T> current) {
    EntryState[] states = new EntryState[list.size()];
    boolean completed = current != null;
    for (int i = 0; i < list.size(); i++) {
      RankElement<T> element = list.get(i);
      if (element == current) {
        states[i] = EntryState.CURRENT;
        completed = false;
      } else if (completed) {
        states[i] = EntryState.COMPLETE;
      } else {
        states[i] = EntryState.LOCKED;
      }
    }
    return states;
  }

  private List<Integer> innerGrid() {
    int rows = size() / 9;
    List<Integer> slots = new ArrayList<>();
    for (int r = 1; r <= rows - 2; r++) {
      for (int c = 1; c <= 7; c++) {
        slots.add(r * 9 + c);
      }
    }
    return slots;
  }
}
