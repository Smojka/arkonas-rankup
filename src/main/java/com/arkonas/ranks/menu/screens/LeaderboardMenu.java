package com.arkonas.ranks.menu.screens;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import com.arkonas.ranks.data.LeaderboardEntry;
import com.arkonas.ranks.data.StatsService;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.menu.MenuText;

/**
 * The rankup/prestige leaderboard: a three-head podium (slots 13/21/23) plus
 * ranks 4-10 in a row, with a toggle between rankups and prestiges. The top list
 * is fetched on the stats thread and applied back on the main thread; a barrier
 * is shown while loading, or when statistics are disabled.
 */
public class LeaderboardMenu extends AbstractMenu {

  private static final int[] PODIUM = {13, 21, 23};
  private static final int[] ENTRY_SLOTS = {28, 29, 30, 31, 32, 33, 34};
  // row 4 centre; slot 49 (the plan's suggestion) is taken by the Home nav button
  private static final int TOGGLE_SLOT = 40;

  @Getter
  private final boolean prestiges;
  @Getter
  private boolean loaded;
  @Getter
  private List<LeaderboardEntry> entries = List.of();

  public LeaderboardMenu(MenuModule module, Player player, AbstractMenu parent, boolean prestiges) {
    super(module, player, parent, module.getConfig().rows("leaderboard", 6));
    this.prestiges = prestiges;
  }

  public int getToggleSlot() {
    return TOGGLE_SLOT;
  }

  @Override
  protected Component title() {
    return text.title(player, text.raw("leaderboard.title", "Leaderboard"), null, null);
  }

  @Override
  protected void populate() {
    StatsService stats = plugin.getStats();
    if (stats == null) {
      setItem(22, icon(Material.BARRIER,
          text.component(player, text.raw("leaderboard.disabled", "&cStatistics are disabled")),
          List.of(), false));
      theme.playSound(player, "deny");
      placeNav(true, getParent() != null, true);
      return;
    }

    setItem(TOGGLE_SLOT, toggleItem());

    if (!loaded) {
      setItem(22, icon(Material.CLOCK,
          text.component(player, text.raw("leaderboard.loading", "&7Loading...")), List.of(), false));
      stats.top(prestiges, 10, result -> Bukkit.getScheduler().runTask(plugin, () -> {
        this.entries = result;
        this.loaded = true;
        if (module.getOpenMenus().contains(this)) {
          refresh();
        }
      }));
    } else {
      placeEntries();
    }

    placeNav(true, getParent() != null, true);
  }

  private void placeEntries() {
    if (entries.isEmpty()) {
      setItem(22, icon(Material.PAPER,
          text.component(player, text.raw("leaderboard.empty", "&7No entries yet")), List.of(), false));
      return;
    }
    for (int i = 0; i < entries.size(); i++) {
      LeaderboardEntry entry = entries.get(i);
      int slot = i < PODIUM.length ? PODIUM[i]
          : (i - PODIUM.length < ENTRY_SLOTS.length ? ENTRY_SLOTS[i - PODIUM.length] : -1);
      if (slot < 0) {
        break;
      }
      setItem(slot, entryHead(i + 1, entry));
    }
  }

  private ItemStack entryHead(int position, LeaderboardEntry entry) {
    Component name = text.component(player,
        MenuText.sub(text.raw("leaderboard.entry", "&e#{position} &f{name} &7- &b{count}"),
            Map.of("position", String.valueOf(position),
                "name", entry.name(),
                "count", String.valueOf(entry.count()))));
    return head(entry.uuid(), entry.name(), name, List.of(), position <= 3);
  }

  private ItemStack toggleItem() {
    String key = prestiges ? "leaderboard.toggle-prestiges" : "leaderboard.toggle-rankups";
    String def = prestiges ? "&dPrestiges &7(click for rankups)"
        : "&aRankups &7(click for prestiges)";
    return icon(Material.COMPARATOR, text.component(player, text.raw(key, def)), List.of(), true);
  }

  @Override
  protected void handleClick(int slot, ClickType click) {
    if (slot == TOGGLE_SLOT) {
      theme.playSound(player, "click");
      defer(() -> new LeaderboardMenu(module, player, getParent(), !prestiges).open());
    }
  }
}
