package com.arkonas.ranks.menu.screens;

import java.util.List;
import org.bukkit.entity.Player;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.ranks.Rankups;
import com.arkonas.ranks.requirements.Requirement;

/**
 * The rankup confirmation screen. Reads the player's position in the rankup
 * ladder and delegates the confirm action to {@code RankupHelper#rankup} so the
 * requirement/cooldown re-check happens exactly as in the classic confirmation GUI.
 */
public class RankupMenu extends ConfirmScreen {

  private final Rankups ladder;

  public RankupMenu(MenuModule module, Player player, AbstractMenu parent) {
    this(module, player, parent, null);
  }

  public RankupMenu(MenuModule module, Player player, AbstractMenu parent, Rankups ladder) {
    super(module, player, parent, module.getConfig().rows("rankup", 5));
    this.ladder = ladder != null ? ladder : plugin.getRankups();
  }

  private RankElement<Rank> element() {
    return ladder == null ? null : ladder.getByPlayer(player);
  }

  @Override
  protected String menuKey() {
    return "rankup";
  }

  @Override
  protected Rank currentRank() {
    RankElement<Rank> element = element();
    return element == null ? null : element.getRank();
  }

  @Override
  protected Rank nextRank() {
    RankElement<Rank> element = element();
    return element != null && element.hasNext() ? element.getNext().getRank() : null;
  }

  @Override
  protected Iterable<Requirement> requirements() {
    Rank current = currentRank();
    if (current == null) {
      return List.of();
    }
    return current.getRequirements().getRequirements(player);
  }

  @Override
  protected boolean requirementsMet() {
    Rank current = currentRank();
    return current != null && current.hasRequirements(player);
  }

  @Override
  protected boolean canAdvance() {
    RankElement<Rank> element = element();
    return element != null && element.hasNext();
  }

  @Override
  protected void performConfirm() {
    // re-checked at the moment of the action, not just when the screen opened: the menu can outlive
    // a permission change, and this screen is reachable from /ranks without rankup.rankup
    if (module.denied(player, com.arkonas.ranks.menu.MenuModule.PERM_RANKUP)) {
      return;
    }
    plugin.getHelper().rankup(player, ladder);
  }
}
