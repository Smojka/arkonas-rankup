package com.arkonas.ranks.menu.screens;

import java.util.ArrayList;
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
  private long lastShownSecond = -1;

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
    lastShownSecond = -1;

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
    List<Requirement> list = new ArrayList<>();
    for (Requirement requirement : requirements()) {
      list.add(requirement);
    }
    List<Integer> slots = requirementSlots(list.size());
    for (int i = 0; i < list.size() && i < slots.size(); i++) {
      setItem(slots.get(i), module.getRequirementRenderer().render(player, list.get(i)));
    }
  }

  private void placeConfirm() {
    setItem(13, targetItem());

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
    cooldownSlot = centerSlot();
    long seconds = (long) Math.ceil(cooldownMillis / 1000.0);
    lastShownSecond = seconds;
    setItem(cooldownSlot, cooldownItem(seconds));
  }

  private ItemStack cooldownItem(long seconds) {
    Component name = text.component(player,
        MenuText.sub(text.raw(menuKey() + ".cooldown", "&eWait &c{seconds}s"),
            Map.of("seconds", String.valueOf(seconds))));
    return icon(Material.CLOCK, name, List.of(), false);
  }

  private ItemStack targetItem() {
    Component name = text.component(player, text.raw(menuKey() + ".target", "&a{{next.rank}}"),
        currentRank(), nextRank());
    return icon(Material.EXPERIENCE_BOTTLE, name, List.of(), true);
  }

  // --- animation ------------------------------------------------------------

  @Override
  protected void onTick(long frame) {
    if (state == State.READY && module.confirmPulse() && confirmSlot >= 0) {
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
      if (seconds != lastShownSecond) {
        setItem(cooldownSlot, cooldownItem(seconds));
        lastShownSecond = seconds;
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
      for (int i = 0; i < count; i++) {
        slots.add(midRow * 9 + start + i);
      }
    } else {
      for (int r = 1; r <= rows - 2 && slots.size() < count; r++) {
        for (int c = 1; c <= 7 && slots.size() < count; c++) {
          slots.add(r * 9 + c);
        }
      }
    }
    return slots;
  }
}
