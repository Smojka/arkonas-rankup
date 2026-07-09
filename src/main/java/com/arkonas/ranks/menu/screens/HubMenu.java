package com.arkonas.ranks.menu.screens;

import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import com.arkonas.ranks.data.StatsService;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.menu.MenuText;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;

/**
 * The central hub: a player-head summary plus buttons to the rank path, rankup,
 * prestige and leaderboard screens. Features that are turned off (prestige,
 * statistics) show a greyed-out button.
 */
public class HubMenu extends AbstractMenu {

  private int pathSlot;
  private int rankupSlot;
  private int prestigeSlot;
  private int leaderboardSlot;

  public HubMenu(MenuModule module, Player player) {
    super(module, player, null, module.getConfig().rows("hub", 5));
  }

  @Override
  protected Component title() {
    return text.title(player, text.raw("hub.title", "Menu"), null, null);
  }

  @Override
  protected void populate() {
    setItem(module.getConfig().slot("hub", "head-slot", 13), headItem());

    this.pathSlot = module.getConfig().slot("hub", "path-slot", 20);
    this.rankupSlot = module.getConfig().slot("hub", "rankup-slot", 22);
    this.prestigeSlot = module.getConfig().slot("hub", "prestige-slot", 24);
    this.leaderboardSlot = module.getConfig().slot("hub", "leaderboard-slot", 40);

    setItem(pathSlot, button(Material.BOOK, "hub.buttons.path.name", "&bRank Path",
        "hub.buttons.path.lore", "&7View the full ladder", true));
    setItem(rankupSlot, button(Material.EMERALD, "hub.buttons.rankup.name", "&aRankup",
        "hub.buttons.rankup.lore", "&7Advance to the next rank", true));

    boolean prestige = plugin.getPrestiges() != null;
    setItem(prestigeSlot, prestige
        ? button(Material.NETHERITE_INGOT, "hub.buttons.prestige.name", "&dPrestige",
            "hub.buttons.prestige.lore", "&7Prestige at the top rank", true)
        : disabledButton(Material.GRAY_DYE, "hub.buttons.prestige.name", "&dPrestige"));

    boolean stats = plugin.getStats() != null;
    setItem(leaderboardSlot, stats
        ? button(Material.GOLD_INGOT, "hub.buttons.leaderboard.name", "&6Leaderboard",
            "hub.buttons.leaderboard.lore", "&7Top rankups & prestiges", true)
        : disabledButton(Material.GRAY_DYE, "hub.buttons.leaderboard.name", "&6Leaderboard"));

    placeNav(false, true, true);
  }

  @Override
  protected void handleClick(int slot, ClickType click) {
    if (slot == pathSlot) {
      theme.playSound(player, "click");
      boolean multi = plugin.getLadders() != null && plugin.getLadders().hasMultiple();
      AbstractMenu next = multi ? new LadderPickerMenu(module, player, this)
          : new RankPathMenu(module, player, this);
      defer(next::open);
    } else if (slot == rankupSlot) {
      theme.playSound(player, "click");
      defer(() -> new RankupMenu(module, player, this).open());
    } else if (slot == prestigeSlot && plugin.getPrestiges() != null) {
      theme.playSound(player, "click");
      defer(() -> new PrestigeMenu(module, player, this).open());
    } else if (slot == leaderboardSlot && plugin.getStats() != null) {
      theme.playSound(player, "click");
      defer(() -> new LeaderboardMenu(module, player, this, false).open());
    }
  }

  private ItemStack headItem() {
    RankElement<Rank> element = plugin.getRankups() == null ? null
        : plugin.getRankups().getByPlayer(player);
    Rank current = element == null ? null : element.getRank();
    Rank next = element != null && element.hasNext() ? element.getNext().getRank() : null;

    String rankName = current == null ? "-" : current.getRank();
    String nextName = next == null ? "-" : next.getRank();

    String rankups = "-";
    String prestiges = "-";
    StatsService stats = plugin.getStats();
    if (stats != null) {
      int[] counts = stats.cachedCounts(player.getUniqueId());
      rankups = String.valueOf(counts[0]);
      prestiges = String.valueOf(counts[1]);
    }

    Component name = text.component(player,
        MenuText.sub(text.raw("hub.head-name", "&b{player}"),
            Map.of("player", player.getName())));

    String rawLore = MenuText.sub(
        text.raw("hub.head-lore", "&7Rank: &f{rank}\n&7Next: &f{next}\n&7Rankups: &a{rankups}"),
        Map.of("rank", rankName, "next", nextName, "rankups", rankups, "prestiges", prestiges));
    List<Component> lore = text.lore(player, rawLore, current, next);

    return head(player.getUniqueId(), player.getName(), name, lore, false);
  }

  private ItemStack button(Material material, String nameKey, String nameDef,
      String loreKey, String loreDef, boolean glow) {
    Component name = text.component(player, text.raw(nameKey, nameDef));
    List<Component> lore = text.lore(player, text.raw(loreKey, loreDef), null, null);
    return icon(material, name, lore, glow);
  }

  private ItemStack disabledButton(Material material, String nameKey, String nameDef) {
    Component name = text.component(player, text.raw(nameKey, nameDef));
    List<Component> lore = text.lore(player, text.raw("hub.disabled", "&8Unavailable"), null, null);
    return icon(material, name, lore, false);
  }
}
