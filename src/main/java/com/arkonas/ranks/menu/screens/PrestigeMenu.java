package com.arkonas.ranks.menu.screens;

import java.util.List;
import org.bukkit.entity.Player;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.prestige.Prestige;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.requirements.Requirement;

/**
 * The prestige confirmation screen. Shares {@link ConfirmScreen} with
 * {@link RankupMenu}; only the ladder source and the confirm action differ.
 * Confirm delegates to {@code RankupHelper#prestige} for the same re-check
 * contract as the classic confirmation GUI.
 */
public class PrestigeMenu extends ConfirmScreen {

  public PrestigeMenu(MenuModule module, Player player, AbstractMenu parent) {
    super(module, player, parent, module.getConfig().rows("prestige", 5));
  }

  private RankElement<Prestige> element() {
    return plugin.getPrestiges() == null ? null : plugin.getPrestiges().getByPlayer(player);
  }

  @Override
  protected String menuKey() {
    return "prestige";
  }

  @Override
  protected Rank currentRank() {
    RankElement<Prestige> element = element();
    return element == null ? null : element.getRank();
  }

  @Override
  protected Rank nextRank() {
    RankElement<Prestige> element = element();
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
    RankElement<Prestige> element = element();
    if (element == null || !element.getRank().isEligible(player)) {
      return false;
    }
    return element.getRank().hasRequirements(player);
  }

  @Override
  protected boolean canAdvance() {
    RankElement<Prestige> element = element();
    return element != null && element.hasNext() && element.getRank().isEligible(player);
  }

  @Override
  protected void performConfirm() {
    plugin.getHelper().prestige(player);
  }
}
