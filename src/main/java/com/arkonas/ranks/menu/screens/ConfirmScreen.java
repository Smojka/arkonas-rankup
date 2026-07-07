package com.arkonas.ranks.menu.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import com.arkonas.ranks.menu.AbstractMenu;
import com.arkonas.ranks.menu.MenuModule;
import com.arkonas.ranks.menu.MenuText;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.requirements.Requirement;

/**
 * Shared base for the rankup and prestige confirmation screens (no fork). Drives
 * the three visible states — UNMET (requirement icons), COOLDOWN (live
 * countdown) and READY (pulsing confirm + cancel) — plus a BLOCKED barrier when
 * there is nothing to advance to. Subclasses supply the ladder facts and the
 * confirm action; the confirm click re-routes through the helper so the exact
 * re-check contract of {@code GuiListener} is preserved.
 */
public abstract class ConfirmScreen extends AbstractMenu {

  public enum State { BLOCKED, UNMET, COOLDOWN, READY }

  @Getter
  private State state = State.BLOCKED;
  @Getter
  private int confirmSlot = -1;
  @Getter
  private int cancelSlot = -1;
  private int cooldownSlot = -1;

  private ItemStack confirmBase;
  private ItemStack confirmGlow;
  private int lastPulsePhase = -1;
  @Getter
  private long shownCooldownSeconds = -1;

  // pre-built fill-reveal variants for the UNMET state (no Pebble in the tick)
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
    super(module, player, parent, rows);
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

    if (current == null || next == null || !canAdvance()) {
      state = State.BLOCKED;
      placeBlocked();
    } else if (!requirementsMet()) {
      state = State.UNMET;
      placeRequirements();
    } else {
      long cooldown = plugin.getHelper().getCooldownRemainingMillis(player.getUniqueId());
      if (cooldown > 0) {
        state = State.COOLDOWN;
        placeCooldown(cooldown);
      } else {
        state = State.READY;
        placeConfirm();
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
    setItem(13, infoPanel());
    fillEntries.clear();
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

  private void placeConfirm() {
    setItem(13, infoPanel());

    confirmBase = icon(Material.LIME_CONCRETE,
        text.component(player, text.raw(menuKey() + ".confirm", "&a&lConfirm"),
            currentRank(), nextRank()),
        text.lore(player, text.raw(menuKey() + ".confirm-lore", "&7Click to confirm"),
            currentRank(), nextRank()),
        false);
    confirmGlow = icon(Material.EMERALD_BLOCK,
        text.component(player, text.raw(menuKey() + ".confirm", "&a&lConfirm"),
            currentRank(), nextRank()),
        text.lore(player, text.raw(menuKey() + ".confirm-lore", "&7Click to confirm"),
            currentRank(), nextRank()),
        true);

    confirmSlot = 29;
    cancelSlot = 33;
    setItem(confirmSlot, confirmBase);
    setItem(cancelSlot, icon(Material.RED_CONCRETE,
        text.component(player, text.raw(menuKey() + ".cancel", "&c&lCancel")),
        List.of(), false));
  }

  private void placeCooldown(long cooldownMillis) {
    setItem(13, infoPanel());
    cooldownSlot = centerSlot();
    long seconds = (long) Math.ceil(cooldownMillis / 1000.0);
    shownCooldownSeconds = seconds;
    setItem(cooldownSlot, cooldownItem(seconds));
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
    String loreRaw = text.raw(menuKey() + ".info-lore",
        "&7From &f{{rank.rank}} &7to &f{{next.rank}}\n&r\n&e&lRewards:\n{rewards}");
    List<Component> lore =
        text.lore(player, MenuText.sub(loreRaw, Map.of("rewards", rewards)), current, next);
    return icon(material, name, lore, true);
  }

  /**
   * Resolves the rewards block: a per-rank {@code rankup.rewards} /
   * {@code prestige.rewards} (or bare {@code rewards}) override on the current
   * rank wins; otherwise the locale {@code rewards-default}.
   */
  private String rewardsBlock(Rank current) {
    ConfigurationSection section = current == null ? null : current.getSection();
    if (section != null) {
      String override = firstNonBlank(
          section.getString(menuKey() + ".rewards"),
          section.getString("rewards"));
      if (override != null) {
        return override;
      }
    }
    return text.raw(menuKey() + ".rewards-default", "&8• &7Unlocks the &f{{next.rank}} &7rank");
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  // --- animation ------------------------------------------------------------

  @Override
  protected void onTick(long frame) {
    if (state == State.UNMET && module.progressFill() && !fillEntries.isEmpty()) {
      long step = frame - getOpenedFrame();
      for (FillEntry entry : fillEntries) {
        int k = (int) Math.max(0, Math.min(step, entry.target));
        if (k != entry.shown) {
          setItem(entry.slot, entry.variants[k]);
          entry.shown = k;
        }
      }
    } else if (state == State.READY && module.confirmPulse() && confirmSlot >= 0) {
      int phase = (int) ((frame / 5) % 2);
      if (phase != lastPulsePhase) {
        setItem(confirmSlot, phase == 0 ? confirmBase : confirmGlow);
        lastPulsePhase = phase;
      }
    } else if (state == State.COOLDOWN && module.cooldownCountdown() && cooldownSlot >= 0) {
      long cooldown = plugin.getHelper().getCooldownRemainingMillis(player.getUniqueId());
      if (cooldown <= 0) {
        refresh();
        return;
      }
      long seconds = (long) Math.ceil(cooldown / 1000.0);
      if (seconds != shownCooldownSeconds) {
        setItem(cooldownSlot, cooldownItem(seconds));
        shownCooldownSeconds = seconds;
      }
    }
  }

  // --- clicks ---------------------------------------------------------------

  @Override
  protected void handleClick(int slot, ClickType click) {
    if (state == State.READY && slot == confirmSlot) {
      theme.playSound(player, "click");
      defer(() -> {
        player.closeInventory();
        performConfirm();
      });
    } else if (slot == cancelSlot) {
      theme.playSound(player, "click");
      defer(() -> {
        if (getParent() != null) {
          getParent().open();
        } else {
          player.closeInventory();
        }
      });
    }
  }

  // --- geometry -------------------------------------------------------------

  private int centerSlot() {
    int rows = size() / 9;
    return (rows / 2) * 9 + 4;
  }

  private List<Integer> requirementSlots(int count) {
    int rows = size() / 9;
    int midRow = rows / 2;
    List<Integer> slots = new ArrayList<>();
    if (count <= 7) {
      int start = 1 + Math.max(0, (7 - count) / 2);
      int c = start;
      while (slots.size() < count) {
        int slot = midRow * 9 + c;
        if (slot != 13) { // reserved for the rank-info panel
          slots.add(slot);
        }
        c++;
      }
    } else {
      for (int r = 1; r <= rows - 2 && slots.size() < count; r++) {
        for (int c = 1; c <= 7 && slots.size() < count; c++) {
          int slot = r * 9 + c;
          if (slot == 13) {
            continue; // reserved for the rank-info panel
          }
          slots.add(slot);
        }
      }
    }
    return slots;
  }
}
