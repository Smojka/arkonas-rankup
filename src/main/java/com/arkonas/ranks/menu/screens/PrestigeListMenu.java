package com.arkonas.ranks.menu.screens;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.prestige.Prestige;
import com.arkonas.ranks.ranks.RankElement;

/**
 * The prestige ladder, following the {@link RankPathMenu} pattern: completed
 * prestiges glow green, the current one is the player's head (click to open the
 * prestige screen) and the rest are locked.
 */
public class PrestigeListMenu extends LadderMenu<Prestige> {

  public PrestigeListMenu(MenuModule module, Player player, AbstractMenu parent) {
    this(module, player, parent, 0);
  }

  public PrestigeListMenu(MenuModule module, Player player, AbstractMenu parent, int page) {
    super(module, player, parent, module.getConfig().rows("prestige-list", 6), page);
  }

  @Override
  protected String menuKey() {
    return "prestige-list";
  }

  @Override
  protected List<RankElement<Prestige>> ladderEntries() {
    List<RankElement<Prestige>> entries = new ArrayList<>();
    if (plugin.getPrestiges() == null) {
      return entries;
    }
    for (RankElement<Prestige> element : plugin.getPrestiges().getTree().asList()) {
      if (element.hasNext()) {
        entries.add(element);
      }
    }
    return entries;
  }

  @Override
  protected RankElement<Prestige> currentEntry() {
    return plugin.getPrestiges() == null ? null : plugin.getPrestiges().getByPlayer(player);
  }

  @Override
  protected ItemStack entryItem(RankElement<Prestige> element, EntryState state) {
    Prestige prestige = element.getRank();
    Prestige next = element.getNext().getRank();
    switch (state) {
      case CURRENT: {
        Component name = text.component(player,
            text.raw("prestige-list.current", "&d{{next.rank}}"), prestige, next);
        List<Component> lore = new ArrayList<>();
        lore.add(text.component(player, text.raw("prestige-list.click", "&eClick to prestige")));
        return head(player.getUniqueId(), player.getName(), name, lore, true);
      }
      case COMPLETE: {
        Component name = text.component(player,
            text.raw("prestige-list.complete", "&a{{rank.rank}} &7(done)"), prestige, next);
        return icon(Material.LIME_STAINED_GLASS_PANE, name, List.of(), true);
      }
      default: {
        Component name = text.component(player,
            text.raw("prestige-list.locked", "&c{{next.rank}}"), prestige, next);
        return icon(Material.RED_STAINED_GLASS_PANE, name, List.of(), false);
      }
    }
  }

  @Override
  protected void onEntryClick(RankElement<Prestige> element, EntryState state) {
    if (state == EntryState.CURRENT) {
      theme.playSound(player, "click");
      defer(() -> new PrestigeMenu(module, player, this).open());
    }
  }

  @Override
  protected LadderMenu<Prestige> pageMenu(int page) {
    return new PrestigeListMenu(module, player, getParent(), page);
  }
}
