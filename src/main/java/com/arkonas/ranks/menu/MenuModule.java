package com.arkonas.ranks.menu;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.menu.screens.HubMenu;
import com.arkonas.ranks.menu.screens.LadderPickerMenu;
import com.arkonas.ranks.menu.screens.LeaderboardMenu;
import com.arkonas.ranks.menu.screens.PrestigeListMenu;
import com.arkonas.ranks.menu.screens.PrestigeMenu;
import com.arkonas.ranks.menu.screens.RankPathMenu;
import com.arkonas.ranks.menu.screens.RankupMenu;

/**
 * Owner/facade for the advanced menu module: loads {@code menus.yml}, wires the
 * {@link MenuTicker} and {@link MenuListener}, exposes the {@code open*}
 * factories used by the command wrappers, and tracks which menus are open.
 *
 * <p>Everything here is additive to the classic chat-based core. When
 * {@code menus.enabled} is false this module is never constructed and the plugin
 * behaves exactly as before.
 */
public class MenuModule {

  @Getter
  private final ArkonasRanksPlugin plugin;
  @Getter
  private final MenuListener listener = new MenuListener(this);

  private MenuConfig config;
  @Getter
  private MenuTheme theme;
  @Getter
  private MenuText text;
  @Getter
  private RequirementIcons icons;
  @Getter
  private RequirementItemRenderer requirementRenderer;
  @Getter
  private RankLore rankLore;
  @Getter
  private RequirementLore requirementLore;
  private MenuTicker ticker;

  private final Set<AbstractMenu> openMenus = new LinkedHashSet<>();

  public MenuModule(ArkonasRanksPlugin plugin) {
    this.plugin = plugin;
    load();
  }

  private void load() {
    this.config = new MenuConfig(plugin);
    this.theme = new MenuTheme(plugin, config);
    this.text = new MenuText(plugin, theme);
    this.icons = new RequirementIcons(config.requirementIcons());
    this.requirementRenderer = new RequirementItemRenderer(this);
    this.requirementLore = new RequirementLore(this);
    this.rankLore = new RankLore(this);
    this.ticker = new MenuTicker(plugin, this, config.animationPeriod());
  }

  public MenuConfig getConfig() {
    return config;
  }

  // --- open set / ticker lifecycle -----------------------------------------

  public Set<AbstractMenu> getOpenMenus() {
    return openMenus;
  }

  void register(AbstractMenu menu) {
    openMenus.add(menu);
    if (config.animationEnabled()) {
      ticker.ensureRunning();
    }
  }

  void forget(AbstractMenu menu) {
    openMenus.remove(menu);
  }

  void forgetPlayer(Player player) {
    openMenus.removeIf(menu -> player.equals(menu.getPlayer()));
  }

  long currentFrame() {
    return ticker.frame();
  }

  // --- animation flags ------------------------------------------------------

  boolean borderChase() {
    return config.animationEnabled() && config.animationFlag("border-chase");
  }

  public boolean progressFill() {
    return config.animationEnabled() && config.animationFlag("progress-fill");
  }

  public boolean confirmPulse() {
    return config.animationEnabled() && config.animationFlag("confirm-pulse");
  }

  public boolean cooldownCountdown() {
    return config.animationEnabled() && config.animationFlag("cooldown-countdown");
  }

  /** Open-reveal transition, opt-in (default off), so existing menus are unchanged. */
  public boolean openReveal() {
    return config.animationEnabled() && config.animationFlag("open-reveal", false);
  }

  public int openRevealSpeed() {
    return config.openRevealSpeed();
  }

  // --- permissions ----------------------------------------------------------

  // The menus stand in for the chat commands, so they must be gated by the SAME permission nodes.
  // Without this a player who only holds rankup.ranks can reach /ranks -> path -> hub -> Rankup ->
  // Confirm and rank up (or prestige) with the matching command permission revoked.
  public static final String PERM_RANKUP = "rankup.rankup";
  public static final String PERM_RANKS = "rankup.ranks";
  public static final String PERM_PRESTIGE = "rankup.prestige";
  public static final String PERM_PRESTIGES = "rankup.prestiges";
  public static final String PERM_TOP = "rankup.top";

  /**
   * Whether the player may use the screen behind {@code permission}. Every {@code open*} factory and
   * every confirm action checks this, so a permission revoked while a menu is open still blocks the
   * action.
   */
  public boolean allowed(Player player, String permission) {
    return player != null && player.hasPermission(permission);
  }

  /** Checks a permission and tells the player when they lack it; true when the click is blocked. */
  public boolean denied(Player player, String permission) {
    if (allowed(player, permission)) {
      return false;
    }
    if (player != null) {
      player.sendMessage(org.bukkit.ChatColor.RED
          + "You do not have permission to do that.");
    }
    return true;
  }

  // --- factories ------------------------------------------------------------

  public void openHub(Player player) {
    new HubMenu(this, player).open();
  }

  public void openRankup(Player player) {
    if (denied(player, PERM_RANKUP)) {
      return;
    }
    new RankupMenu(this, player, null).open();
  }

  public void openRankPath(Player player) {
    if (denied(player, PERM_RANKS)) {
      return;
    }
    // multi-ladder servers get a ladder picker; single-ladder servers go straight to the path
    if (plugin.getLadders() != null && plugin.getLadders().hasMultiple()) {
      new LadderPickerMenu(this, player, null).open();
    } else {
      new RankPathMenu(this, player, null).open();
    }
  }

  public void openLadderPicker(Player player) {
    if (denied(player, PERM_RANKS)) {
      return;
    }
    new LadderPickerMenu(this, player, null).open();
  }

  public void openPrestige(Player player) {
    if (denied(player, PERM_PRESTIGE)) {
      return;
    }
    new PrestigeMenu(this, player, null).open();
  }

  public void openPrestigeList(Player player) {
    if (denied(player, PERM_PRESTIGES)) {
      return;
    }
    new PrestigeListMenu(this, player, null).open();
  }

  public void openLeaderboard(Player player, boolean prestiges) {
    if (denied(player, PERM_TOP)) {
      return;
    }
    new LeaderboardMenu(this, player, null, prestiges).open();
  }

  // --- lifecycle ------------------------------------------------------------

  /** Reloads {@code menus.yml}: cancel the ticker first, then close everything. */
  public void reload() {
    ticker.stop();
    closeAll();
    load();
  }

  /** Closes every open menu (main thread). */
  public void closeAll() {
    ticker.stop();
    for (AbstractMenu menu : new LinkedHashSet<>(openMenus)) {
      Player player = menu.getPlayer();
      if (player != null && player.isOnline()) {
        player.closeInventory();
      }
    }
    openMenus.clear();
  }
}
