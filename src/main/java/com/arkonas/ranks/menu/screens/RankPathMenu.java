package com.arkonas.ranks.menu.screens;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.menu.MenuText;
import com.arkonas.ranks.menu.ProgressBar;
import com.arkonas.ranks.menu.RequirementItemRenderer;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.requirements.Requirement;

/**
 * The full rankup ladder as a paginated map: completed ranks are glowing green
 * panes, the current rank is the player's head (click to open the rankup
 * screen) and locked ranks are red panes with their costs.
 */
public class RankPathMenu extends LadderMenu<Rank> {

  public RankPathMenu(MenuModule module, Player player, AbstractMenu parent) {
    this(module, player, parent, 0);
  }

  public RankPathMenu(MenuModule module, Player player, AbstractMenu parent, int page) {
    super(module, player, parent, module.getConfig().rows("path", 6), page);
  }

  @Override
  protected String menuKey() {
    return "path";
  }

  @Override
  protected List<RankElement<Rank>> ladderEntries() {
    List<RankElement<Rank>> entries = new ArrayList<>();
    if (plugin.getRankups() == null) {
      return entries;
    }
    for (RankElement<Rank> element : plugin.getRankups().getTree().asList()) {
      if (element.hasNext()) {
        entries.add(element);
      }
    }
    return entries;
  }

  @Override
  protected RankElement<Rank> currentEntry() {
    return plugin.getRankups() == null ? null : plugin.getRankups().getByPlayer(player);
  }

  @Override
  protected ItemStack entryItem(RankElement<Rank> element, EntryState state) {
    Rank rank = element.getRank();
    Rank next = element.getNext().getRank();
    switch (state) {
      case CURRENT: {
        Component name = text.component(player,
            text.raw("path.current", "&d{{rank.rank}} &7» &f{{next.rank}}"), rank, next);
        List<Component> lore = requirementLines(element, true);
        lore.add(text.component(player, text.raw("path.click-rankup", "&eClick to rankup")));
        return head(player.getUniqueId(), player.getName(), name, lore, true);
      }
      case COMPLETE: {
        Component name = text.component(player,
            text.raw("path.complete", "&a{{rank.rank}} &7(completed)"), rank, next);
        return icon(Material.LIME_STAINED_GLASS_PANE, name, List.of(), true);
      }
      default: {
        Component name = text.component(player,
            text.raw("path.locked", "&c{{rank.rank}} &7» &f{{next.rank}}"), rank, next);
        return icon(Material.RED_STAINED_GLASS_PANE, name, requirementLines(element, false), false);
      }
    }
  }

  @Override
  protected void onEntryClick(RankElement<Rank> element, EntryState state) {
    if (state == EntryState.CURRENT) {
      theme.playSound(player, "click");
      defer(() -> new RankupMenu(module, player, this).open());
    }
  }

  @Override
  protected LadderMenu<Rank> pageMenu(int page) {
    return new RankPathMenu(module, player, getParent(), page);
  }

  private List<Component> requirementLines(RankElement<Rank> element, boolean showProgress) {
    List<Component> lines = new ArrayList<>();
    Rank rank = element.getRank();
    RequirementItemRenderer renderer = module.getRequirementRenderer();
    for (Requirement requirement : rank.getRequirements().getRequirements(player)) {
      boolean money = RequirementItemRenderer.isMoney(requirement.getName());
      double total = requirement.getTotal(player);
      String friendly = renderer.friendlyName(requirement.getName());
      String filter = money ? "money" : "simple";
      String line = "&7• &f" + friendly + ": &b{{ value | " + filter + " }}";
      if (showProgress) {
        double remaining = requirement.getRemaining(player);
        double fraction = total <= 0 ? 1 : Math.max(0, total - remaining) / total;
        line = MenuText.sub(line + " &7({percent}%)",
            java.util.Map.of("percent", String.valueOf(ProgressBar.percent(fraction))));
      }
      String finalLine = line;
      lines.add(text.component(player, finalLine, mb -> mb.replaceKey("value", total)));
    }
    return lines;
  }
}
