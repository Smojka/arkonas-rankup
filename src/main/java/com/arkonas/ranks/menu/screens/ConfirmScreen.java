package com.arkonas.ranks.menu.screens;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.menu.MenuText;
import com.arkonas.ranks.menu.RankLore;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.requirements.Requirement;

/**
 * Shared base for the rankup and prestige confirmation screens (no fork). Drives
 * the three visible states — UNMET (locked button), COOLDOWN (live countdown)
 * and READY (pulsing confirm) — plus a BLOCKED barrier when there is nothing to
 * advance to. Subclasses supply the ladder facts and the confirm action; the
 * confirm click re-routes through the helper so the exact re-check contract of
 * {@code GuiListener} is preserved.
 *
 * <p>Every reachable state draws the <em>same</em> grid — info panel on top, the
 * requirement icons mirrored around the centre column, one action button below
 * them — so nothing jumps around as the player's progress changes. Only the
 * action button differs: locked (with the missing requirements in its lore),
 * counting down, or the green confirm.
 */
public abstract class ConfirmScreen extends AbstractMenu {

  public enum State { BLOCKED, UNMET, COOLDOWN, READY }

  /** The rank-info panel: top interior row, centre column. */
  private static final int INFO_SLOT = 13;
  /** Expanded to the requirements the player is still missing, inside the locked lore. */
  private static final String REQUIREMENTS_TOKEN = "{requirements}";
  private static final String DEFAULT_LOCKED_LORE =
      "&7You are still missing:\n" + REQUIREMENTS_TOKEN
          + "\n&r\n&8Finish these and this button turns green.";

  @Getter
  private State state = State.BLOCKED;
  /**
   * The single action button, at the same slot in UNMET, COOLDOWN and READY (-1 only while
   * BLOCKED). Clicking it ranks the player up when they qualify and is refused otherwise.
   */
  @Getter
  private int actionSlot = -1;
  /** The action slot while it actually confirms, i.e. only in the READY state. */
  @Getter
  private int confirmSlot = -1;

  private ItemStack confirmBase;
  private ItemStack confirmGlow;
  /** Set once the confirm click has been accepted, so repeat clicks in the same tick are ignored. */
  private boolean confirmed;
  private int lastPulsePhase = -1;
  @Getter
  private long shownCooldownSeconds = -1;

  // pre-built fill-reveal variants for the requirement icons (no Pebble in the tick)
  private final List<FillEntry> fillEntries = new ArrayList<>();

  private static final class FillEntry {
    final int slot;
    final ItemStack[] variants;
    final int target;
    int shown = -1;

    FillEntry(int slot, ItemStack[] variants, int target) {
      this.slot = slot;
      this.variants = variants;
      this.target = target;
    }
  }

  protected ConfirmScreen(MenuModule module, Player player, AbstractMenu parent, int rows) {
    // the layout needs a top row for the panel, a requirement row and a row for the action
    // button, so a misconfigured `rows:` is clamped rather than left to collapse into itself
    super(module, player, parent, Math.min(6, Math.max(4, rows)));
  }

  // --- subclass contract ----------------------------------------------------

  protected abstract String menuKey();

  protected abstract Rank currentRank();

  protected abstract Rank nextRank();

  protected abstract Iterable<Requirement> requirements();

  protected abstract boolean requirementsMet();

  protected abstract boolean canAdvance();

  protected abstract void performConfirm();

  // --- layout ---------------------------------------------------------------

  @Override
  protected Component title() {
    return text.title(player, text.raw(menuKey() + ".title", "Rankup"), currentRank(), nextRank());
  }

  @Override
  protected void populate() {
    Rank current = currentRank();
    Rank next = nextRank();
    lastPulsePhase = -1;
    shownCooldownSeconds = -1;
    confirmed = false;
    confirmSlot = -1;
    actionSlot = -1;
    fillEntries.clear();

    if (current == null || next == null || !canAdvance()) {
      state = State.BLOCKED;
      placeBlocked();
    } else {
      actionSlot = resolveActionSlot();
      setItem(INFO_SLOT, infoPanel());
      placeRequirements();
      if (!requirementsMet()) {
        state = State.UNMET;
        placeLocked(current, next);
      } else {
        long cooldown = plugin.getHelper().getCooldownRemainingMillis(player.getUniqueId());
        if (cooldown > 0) {
          state = State.COOLDOWN;
          placeCooldown(cooldown);
        } else {
          state = State.READY;
          placeConfirm(current, next);
        }
      }
    }

    placeNav(true, getParent() != null, true);
  }

  private void placeBlocked() {
    setItem(centerSlot(), icon(Material.BARRIER,
        text.component(player, text.raw(menuKey() + ".blocked", "&cNothing to advance to"),
            currentRank(), nextRank()),
        List.of(), false));
  }

  private void placeRequirements() {
    List<Requirement> list = new ArrayList<>();
    for (Requirement requirement : requirements()) {
      list.add(requirement);
    }
    List<Integer> slots = requirementSlots(list.size());
    var renderer = module.getRequirementRenderer();
    boolean fill = module.progressFill();
    for (int i = 0; i < list.size() && i < slots.size(); i++) {
      int slot = slots.get(i);
      Requirement requirement = list.get(i);
      int target = renderer.targetSegments(player, requirement);
      if (fill && target > 0) {
        // pre-build one item per reveal step so the tick never runs Pebble
        ItemStack[] variants = new ItemStack[target + 1];
        for (int k = 0; k <= target; k++) {
          variants[k] = renderer.render(player, requirement, k);
        }
        fillEntries.add(new FillEntry(slot, variants, target));
        setItem(slot, variants[0]);
      } else {
        setItem(slot, renderer.render(player, requirement));
      }
    }
  }

  /**
   * The action button while the player does not qualify: it stays in place and in reach, but
   * says in its own lore which requirements are still missing instead of firing.
   */
  private void placeLocked(Rank current, Rank next) {
    Material material =
        module.getConfig().material(menuKey(), "locked-material", Material.RED_CONCRETE);
    Component name = text.component(player,
        text.raw(menuKey() + ".locked", "&c&lRequirements not met"), current, next);
    setItem(actionSlot, icon(material, name, lockedLore(current, next), false));
  }

  /**
   * The locked button's lore: the locale template, with {@code {requirements}} expanded to the
   * requirement lines the player has not finished yet (the met ones are dropped whatever
   * {@code menu.requirements.hide-met} says — this block exists to name what is missing).
   */
  private List<Component> lockedLore(Rank current, Rank next) {
    String raw = text.raw(menuKey() + ".locked-lore", DEFAULT_LOCKED_LORE);
    List<Component> lore = new ArrayList<>();
    List<Component> unmet = null;
    for (String line : raw.split("\n", -1)) {
      if (line.trim().equals(REQUIREMENTS_TOKEN)) {
        if (unmet == null) {
          unmet = module.getRankLore().unmetRequirementLines(player, current, next);
        }
        lore.addAll(unmet);
        continue;
      }
      lore.add(text.component(player, line, current, next));
    }
    return lore;
  }

  private void placeConfirm(Rank current, Rank next) {
    String name = text.raw(menuKey() + ".confirm", "&a&lConfirm");
    String lore = text.raw(menuKey() + ".confirm-lore", "&7Click to confirm");
    confirmBase = icon(Material.LIME_CONCRETE, text.component(player, name, current, next),
        text.lore(player, lore, current, next), false);
    confirmGlow = icon(Material.EMERALD_BLOCK, text.component(player, name, current, next),
        text.lore(player, lore, current, next), true);

    confirmSlot = actionSlot;
    setItem(confirmSlot, confirmBase);
  }

  private void placeCooldown(long cooldownMillis) {
    long seconds = (long) Math.ceil(cooldownMillis / 1000.0);
    shownCooldownSeconds = seconds;
    setItem(actionSlot, cooldownItem(seconds));
  }

  private ItemStack cooldownItem(long seconds) {
    Component name = text.component(player,
        MenuText.sub(text.raw(menuKey() + ".cooldown", "&eWait &c{seconds}s"),
            Map.of("seconds", String.valueOf(seconds))));
    return icon(Material.CLOCK, name, List.of(), false);
  }

  /**
   * The rank-info panel shown at slot 13 in the UNMET, COOLDOWN and READY states
   * (never BLOCKED). Names the rank being advanced to and lists the rewards
   * gained there. Built once per populate/refresh — never in {@link #onTick} — so
   * the animation tick never runs Pebble and the dirty-slot contract holds.
   */
  private ItemStack infoPanel() {
    Rank current = currentRank();
    Rank next = nextRank();
    Material material =
        module.getConfig().material(menuKey(), "info-material", Material.WRITABLE_BOOK);
    String nameDefault =
        "prestige".equals(menuKey())
            ? "&d&l{{ next.name | default(next.rank) }}"
            : "&b&l{{ next.name | default(next.rank) }}";
    Component name =
        text.component(player, text.raw(menuKey() + ".info-name", nameDefault), current, next);
    String rewards = rewardsBlock(current);
    RankLore rankLore = module.getRankLore();
    // a hand-written `lore:` on the rank replaces the locale info-lore entirely
    List<Component> lore = rankLore.render(player, current, next, RankLore.INFO,
        () -> rankLore.requirementLines(player, current, next, true), rewards);
    if (lore == null) {
      String loreRaw = text.raw(menuKey() + ".info-lore",
          "&7From &f{{rank.rank}} &7to &f{{next.rank}}\n&r\n&e&lRewards:\n{rewards}");
      lore = text.lore(player, MenuText.sub(loreRaw, Map.of("rewards", rewards)), current, next);
    }
    return icon(material, name, lore, true);
  }

  /**
   * Resolves the rewards block: a per-rank {@code rankup.rewards} /
   * {@code prestige.rewards} (or bare {@code rewards}) override on the current
   * rank wins; otherwise the locale {@code rewards-default}.
   */
  private String rewardsBlock(Rank current) {
    return module.getRankLore().rewards(current, menuKey());
  }

  // --- animation ------------------------------------------------------------

  @Override
  protected void onTick(long frame) {
    if (module.progressFill() && !fillEntries.isEmpty()) {
      // key off reveal completion so the fill animates from 0 after any open-reveal wipe
      long step = frame - getRevealDoneFrame();
      for (FillEntry entry : fillEntries) {
        int k = (int) Math.max(0, Math.min(step, entry.target));
        if (k != entry.shown) {
          setItem(entry.slot, entry.variants[k]);
          entry.shown = k;
        }
      }
    }
    if (state == State.READY && module.confirmPulse() && confirmSlot >= 0) {
      int phase = (int) ((frame / 5) % 2);
      if (phase != lastPulsePhase) {
        setItem(confirmSlot, phase == 0 ? confirmBase : confirmGlow);
        lastPulsePhase = phase;
      }
    } else if (state == State.COOLDOWN && module.cooldownCountdown() && actionSlot >= 0) {
      long cooldown = plugin.getHelper().getCooldownRemainingMillis(player.getUniqueId());
      if (cooldown <= 0) {
        refresh();
        return;
      }
      long seconds = (long) Math.ceil(cooldown / 1000.0);
      if (seconds != shownCooldownSeconds) {
        setItem(actionSlot, cooldownItem(seconds));
        shownCooldownSeconds = seconds;
      }
    }
  }

  // --- clicks ---------------------------------------------------------------

  @Override
  protected void handleClick(int slot, ClickType click) {
    if (actionSlot < 0 || slot != actionSlot) {
      return;
    }
    if (state != State.READY) {
      // the button is on screen in every state, so a click that cannot rank the player up has to
      // answer for itself; the lore already says what is missing
      theme.playSound(player, "deny");
      return;
    }
    // one confirm per screen: the action is deferred to the next tick, and a client can deliver
    // several click packets within the same tick, so without this a click-spamming player queues
    // several rankups off a single screen (each one charges them again)
    if (confirmed) {
      return;
    }
    confirmed = true;
    theme.playSound(player, "click");
    defer(() -> {
      player.closeInventory();
      performConfirm();
    });
  }

  // --- geometry -------------------------------------------------------------

  private int centerSlot() {
    int rows = size() / 9;
    return (rows / 2) * 9 + 4;
  }

  /**
   * Where the action button lives: the centre of the row under the requirement row, or the
   * {@code action-slot} override from menus.yml. It is the same slot in every state so the
   * button never moves out from under the cursor.
   */
  private int resolveActionSlot() {
    int rows = size() / 9;
    int midRow = rows / 2;
    // a 4-row menu has no interior row below the middle one, so the button shares that row and
    // the requirement icons mirror around it
    int row = midRow + 1 <= rows - 2 ? midRow + 1 : midRow;
    int def = row * 9 + 4;
    int configured = module.getConfig().slot(menuKey(), "action-slot", def);
    return configured >= 0 && configured < size() ? configured : def;
  }

  /**
   * Mirror-symmetric slots for {@code count} requirement icons. Every row is centred on column 4
   * and rows are used outward from the middle one, so the block always reads as balanced: an even
   * number of icons straddles the centre column instead of leaning one slot to the left.
   */
  private List<Integer> requirementSlots(int count) {
    List<Integer> slots = new ArrayList<>();
    if (count <= 0) {
      return slots;
    }
    List<int[]> shares = new ArrayList<>(); // {row, icons placed on it}
    int remaining = count;
    for (int row : interiorRowsFromMiddle(size() / 9)) {
      if (remaining <= 0) {
        break;
      }
      int take = Math.min(rowCapacity(row), remaining);
      shares.add(new int[] {row, take});
      remaining -= take;
    }
    shares.sort(Comparator.comparingInt(share -> share[0]));
    for (int[] share : shares) {
      for (int column : centredColumns(share[1], centreFree(share[0]))) {
        slots.add(share[0] * 9 + column);
      }
    }
    return slots;
  }

  /** Interior rows (no border, no nav bar), ordered outward from the middle: 2, 1, 3 on 5 rows. */
  private static List<Integer> interiorRowsFromMiddle(int rows) {
    List<Integer> order = new ArrayList<>();
    int mid = rows / 2;
    order.add(mid);
    for (int distance = 1; distance < rows; distance++) {
      if (mid - distance >= 1) {
        order.add(mid - distance);
      }
      if (mid + distance <= rows - 2) {
        order.add(mid + distance);
      }
    }
    return order;
  }

  /** How many icons one row can hold symmetrically: 7, or 6 when its centre slot is taken. */
  private int rowCapacity(int row) {
    return centreFree(row) ? 7 : 6;
  }

  /** Whether a row's centre column is free, i.e. holds neither the info panel nor the button. */
  private boolean centreFree(int row) {
    int centre = row * 9 + 4;
    return centre != INFO_SLOT && centre != actionSlot;
  }

  /**
   * {@code count} columns mirrored around column 4. An odd count sits on the centre column; an
   * even one — and any count on a row whose centre is already taken — splits into two equal
   * halves around it, which is what keeps the row symmetric.
   */
  private static int[] centredColumns(int count, boolean centreFree) {
    int[] columns = new int[count];
    boolean useCentre = centreFree && count % 2 == 1;
    int side = useCentre ? (count - 1) / 2 : count / 2;
    int i = 0;
    for (int column = 4 - side; column < 4; column++) {
      columns[i++] = column;
    }
    if (useCentre) {
      columns[i++] = 4;
    }
    for (int column = 5; i < count; column++) {
      columns[i++] = column;
    }
    return columns;
  }
}
