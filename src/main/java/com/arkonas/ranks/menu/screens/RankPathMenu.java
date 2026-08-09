package com.arkonas.ranks.menu.screens;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.menu.RankLore;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.ranks.Rankups;

/**
 * The full rankup ladder as a paginated map: completed ranks are glowing green
 * panes, the current rank is the player's head (click to open the rankup
 * screen) and locked ranks are red panes with their costs.
 */
public class RankPathMenu extends LadderMenu<Rank> {

  /** The ladder this path shows; defaults to the primary ladder for single-ladder servers. */
  private final Rankups ladder;

  public RankPathMenu(MenuModule module, Player player, AbstractMenu parent) {
    this(module, player, parent, null, 0);
  }

  public RankPathMenu(MenuModule module, Player player, AbstractMenu parent, Rankups ladder) {
    this(module, player, parent, ladder, 0);
  }

  public RankPathMenu(MenuModule module, Player player, AbstractMenu parent, Rankups ladder,
      int page) {
    super(module, player, parent, module.getConfig().rows("path", 6), page);
    this.ladder = ladder != null ? ladder : plugin.getRankups();
  }

  @Override
  protected String menuKey() {
    return "path";
  }

  @Override
  protected List<RankElement<Rank>> ladderEntries() {
    List<RankElement<Rank>> entries = new ArrayList<>();
    if (ladder == null) {
      return entries;
    }
    for (RankElement<Rank> element : ladder.getTree().asList()) {
      if (element.hasNext()) {
        entries.add(element);
      }
    }
    return entries;
  }

  @Override
  protected RankElement<Rank> currentEntry() {
    return ladder == null ? null : ladder.getByPlayer(player);
  }

  @Override
  protected ItemStack entryItem(RankElement<Rank> element, EntryState state) {
    Rank rank = element.getRank();
    Rank next = element.getNext().getRank();
    switch (state) {
      case CURRENT: {
        Component name = text.component(player,
            text.raw("path.current", "&d{{rank.rank}} &7» &f{{next.rank}}"), rank, next);
        List<Component> lore = manualLore(rank, next, RankLore.CURRENT, true);
        if (lore == null) {
          lore = requirementLines(element, true);
          lore.add(text.component(player, text.raw("path.click-rankup", "&eClick to rankup")));
        }
        return head(player.getUniqueId(), player.getName(), name, lore, true);
      }
      case COMPLETE: {
        Component name = text.component(player,
            text.raw("path.complete", "&a{{rank.rank}} &7(completed)"), rank, next);
        List<Component> lore = manualLore(rank, next, RankLore.COMPLETE, false);
        return icon(Material.LIME_STAINED_GLASS_PANE, name, lore == null ? List.of() : lore, true);
      }
      default: {
        Component name = text.component(player,
            text.raw("path.locked", "&c{{rank.rank}} &7» &f{{next.rank}}"), rank, next);
        List<Component> lore = manualLore(rank, next, RankLore.LOCKED, false);
        if (lore == null) {
          lore = requirementLines(element, false);
        }
        return icon(Material.RED_STAINED_GLASS_PANE, name, lore, false);
      }
    }
  }

  /** The rank's hand-written {@code lore:} from rankups.yml, or null when it has none. */
  private List<Component> manualLore(Rank rank, Rank next, String variant, boolean showProgress) {
    RankLore lore = module.getRankLore();
    return lore.render(player, rank, next, variant,
        () -> lore.requirementLines(player, rank, next, showProgress),
        lore.rewards(rank, "rankup"));
  }

  @Override
  protected void onEntryClick(RankElement<Rank> element, EntryState state) {
    if (state == EntryState.CURRENT) {
      // the path is reachable with only rankup.ranks; ranking up from it still needs rankup.rankup
      if (module.denied(player, com.arkonas.ranks.menu.MenuModule.PERM_RANKUP)) {
        return;
      }
      theme.playSound(player, "click");
      defer(() -> new RankupMenu(module, player, this, ladder).open());
    }
  }

  @Override
  protected LadderMenu<Rank> pageMenu(int page) {
    return new RankPathMenu(module, player, getParent(), ladder, page);
  }

  private List<Component> requirementLines(RankElement<Rank> element, boolean showProgress) {
    Rank next = element.hasNext() ? element.getNext().getRank() : null;
    return module.getRankLore().requirementLines(player, element.getRank(), next, showProgress);
  }
}
