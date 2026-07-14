package com.arkonas.ranks.menu;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.arkonas.ranks.ArkonasRanksPlugin;

/**
 * Base class for every menu screen. Owns the {@link Inventory} (as its own
 * {@link InventoryHolder}), the decorative border ring and the bottom-row
 * navigation bar (Home / Back / Close). Subclasses implement {@link #title()}
 * and {@link #populate()} and may override {@link #onTick(long)} and
 * {@link #handleClick(int, ClickType)}.
 *
 * <p>The border "chase" light moves every frame and only rewrites the two rings
 * of slots that change, so the ticker never allocates and never disturbs the
 * functional item slots (guaranteeing the dirty-slot contract).
 */
public abstract class AbstractMenu implements InventoryHolder {

  protected final ArkonasRanksPlugin plugin;
  protected final MenuModule module;
  protected final MenuTheme theme;
  protected final MenuText text;
  @Getter
  protected final Player player;
  @Getter
  protected final AbstractMenu parent;

  private final int rows;
  private Inventory inventory;
  @Getter
  private long openedFrame;

  private int[] ringSlots = new int[0];
  private int backSlot = -1;
  private int homeSlot = -1;
  private int closeSlot = -1;

  // open-reveal transition state
  private ItemStack[] revealItems;
  private boolean revealing;
  // frame at which content became fully visible (== openedFrame unless a reveal delayed it);
  // content animations (e.g. the confirm progress-fill) should key their step off this
  @Getter
  private long revealDoneFrame;

  protected AbstractMenu(MenuModule module, Player player, AbstractMenu parent, int rows) {
    this.module = module;
    this.plugin = module.getPlugin();
    this.theme = module.getTheme();
    this.text = module.getText();
    this.player = player;
    this.parent = parent;
    this.rows = rows;
  }

  @NotNull
  @Override
  public Inventory getInventory() {
    return inventory;
  }

  protected int size() {
    return rows * 9;
  }

  /** The gradient inventory title. */
  protected abstract Component title();

  /** Places the functional items (content + nav bar). Called by {@link #build()}. */
  protected abstract void populate();

  /** Per-frame animation hook for subclasses; default does nothing. */
  protected void onTick(long frame) {
  }

  /** Routes a click on a non-navigation slot; default does nothing. */
  protected void handleClick(int slot, ClickType click) {
  }

  /** Cleanup hook fired on inventory close/quit; default does nothing. */
  protected void onClose() {
  }

  /** Builds the inventory and shows it to the player. */
  public final void open() {
    this.openedFrame = module.currentFrame();
    this.revealDoneFrame = this.openedFrame;
    this.inventory = Bukkit.createInventory(this, size(), title());
    layout();
    if (module.openReveal()) {
      // snapshot the built grid, then blank the inventory so the ticker can wipe it in
      this.revealItems = inventory.getContents().clone();
      this.revealing = true;
      for (int slot = 0; slot < size(); slot++) {
        inventory.setItem(slot, theme.borderBase());
      }
    }
    module.register(this);
    player.openInventory(inventory);
    theme.playSound(player, "open");
  }

  /**
   * Repopulates the current inventory in place (no reopen, no flicker). Used for
   * state transitions such as cooldown-expiry -&gt; ready.
   */
  protected final void refresh() {
    if (inventory == null) {
      return;
    }
    // an explicit state refresh cancels any in-progress reveal and shows the fresh grid now
    revealing = false;
    revealItems = null;
    inventory.clear();
    layout();
  }

  private void layout() {
    this.backSlot = -1;
    this.homeSlot = -1;
    this.closeSlot = -1;

    populate();

    // record which border slots are still empty before the base fill, then fill
    // every empty slot with the opaque base pane (interior fillers are static).
    List<Integer> ring = new ArrayList<>();
    for (int slot : borderSlotsClockwise()) {
      if (inventory.getItem(slot) == null) {
        ring.add(slot);
      }
    }
    for (int slot = 0; slot < size(); slot++) {
      if (inventory.getItem(slot) == null) {
        inventory.setItem(slot, theme.borderBase());
      }
    }
    this.ringSlots = ring.stream().mapToInt(Integer::intValue).toArray();
  }

  /** Called by the ticker each frame while this menu is open. */
  final void tick(long frame) {
    if (revealing) {
      // reveal owns the whole grid until complete, so border-chase/fill start only after
      if (advanceReveal(frame)) {
        return;
      }
      revealing = false;
      revealItems = null;
      revealDoneFrame = frame; // content animations key their step off completion
    }
    if (module.borderChase() && ringSlots.length > 0) {
      animateBorder(frame);
    }
    onTick(frame);
  }

  /**
   * Reveals the pre-built grid one row-major band per frame. Only sets the newly revealed slots to
   * their final (pre-built) items; already-revealed slots are untouched and not-yet-revealed slots
   * keep the base pane placed at {@link #open()}. Returns true while the reveal is still running.
   */
  private boolean advanceReveal(long frame) {
    int speed = Math.max(1, module.openRevealSpeed());
    long elapsed = frame - openedFrame; // first tick after open() = 1
    int revealed = (int) Math.min(size(), Math.max(0, elapsed) * speed);
    for (int slot = 0; slot < revealed; slot++) {
      inventory.setItem(slot, revealItems[slot]);
    }
    return revealed < size();
  }

  /** Snaps the reveal to fully visible (used when the player clicks mid-reveal). */
  private void finishReveal() {
    if (revealItems != null) {
      for (int slot = 0; slot < size() && slot < revealItems.length; slot++) {
        inventory.setItem(slot, revealItems[slot]);
      }
      revealDoneFrame = module.currentFrame();
    }
    revealing = false;
    revealItems = null;
  }

  private void animateBorder(long frame) {
    int ring = ringSlots.length;
    int lights = Math.min(theme.lights(), ring);
    int step = Math.max(1, ring / Math.max(1, lights));
    // reset the previous frame's light slots to the base pane
    for (int i = 0; i < lights; i++) {
      int old = ringSlots[(int) Math.floorMod(frame - 1 + (long) i * step, ring)];
      inventory.setItem(old, theme.borderBase());
    }
    // light up this frame's slots
    for (int i = 0; i < lights; i++) {
      int pos = ringSlots[(int) Math.floorMod(frame + (long) i * step, ring)];
      inventory.setItem(pos, theme.borderLight(i));
    }
  }

  final void closed() {
    onClose();
  }

  // --- navigation -----------------------------------------------------------

  /**
   * Places the bottom-row navigation bar. Back is only shown when a parent
   * exists; Home is hidden on the hub itself.
   */
  protected void placeNav(boolean home, boolean back, boolean close) {
    int bottom = (rows - 1) * 9;
    if (back && parent != null) {
      backSlot = bottom;
      inventory.setItem(backSlot, navButton(Material.ARROW,
          text.raw("common.back", "&7« Back"), theme.muted()));
    }
    if (home) {
      homeSlot = bottom + 4;
      inventory.setItem(homeSlot, navButton(Material.NETHER_STAR,
          text.raw("common.home", "&b☰ Menu"), theme.primary()));
    }
    if (close) {
      closeSlot = bottom + 8;
      inventory.setItem(closeSlot, navButton(Material.BARRIER,
          text.raw("common.close", "&cClose"), theme.danger()));
    }
  }

  final void onClick(int slot, ClickType click) {
    // a click during the reveal snaps the grid fully visible and is otherwise swallowed, so a
    // click on a not-yet-revealed slot can never fire a hidden confirm/nav action
    if (revealing) {
      finishReveal();
      return;
    }
    if (slot == closeSlot) {
      theme.playSound(player, "click");
      defer(player::closeInventory);
      return;
    }
    if (slot == homeSlot) {
      theme.playSound(player, "click");
      defer(() -> module.openHub(player));
      return;
    }
    if (slot == backSlot && parent != null) {
      theme.playSound(player, "click");
      defer(parent::open);
      return;
    }
    handleClick(slot, click);
  }

  // --- item helpers ---------------------------------------------------------

  protected void setItem(int slot, ItemStack item) {
    if (slot >= 0 && slot < size()) {
      inventory.setItem(slot, item);
    }
  }

  protected ItemStack icon(Material material, Component name, List<Component> lore, boolean glow) {
    return MenuItems.build(material, name, lore, glow);
  }

  /** A player head owned by the given profile (uuid+name), best-effort skinned. */
  protected ItemStack head(java.util.UUID uuid, String name, Component displayName,
      List<Component> lore, boolean glow) {
    ItemStack item = new ItemStack(Material.PLAYER_HEAD);
    if (item.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta skull) {
      try {
        skull.setOwnerProfile(Bukkit.createProfile(uuid, name));
      } catch (Throwable ignored) {
        // MockBukkit or old API may not support profiles; leave the head blank
      }
      if (displayName != null) {
        skull.displayName(displayName);
      }
      if (lore != null && !lore.isEmpty()) {
        skull.lore(lore);
      }
      if (glow) {
        MenuItems.applyGlow(skull);
      }
      item.setItemMeta(skull);
    }
    return item;
  }

  private ItemStack navButton(Material material, String rawName, String colour) {
    return navItem(material, rawName, colour);
  }

  /** Builds a coloured navigation/utility button (name only, no lore). */
  protected ItemStack navItem(Material material, String rawName, String colour) {
    Component name = text.component(player, "<" + colour + ">" + rawName);
    return icon(material, name, List.of(), false);
  }

  protected void defer(Runnable runnable) {
    Bukkit.getScheduler().runTask(plugin, (Runnable) runnable::run);
  }

  // --- border geometry ------------------------------------------------------

  private int[] borderSlotsClockwise() {
    if (rows < 1) {
      return new int[0];
    }
    List<Integer> slots = new ArrayList<>();
    int last = rows - 1;
    // top row left -> right
    for (int c = 0; c < 9; c++) {
      slots.add(c);
    }
    if (rows > 1) {
      // right column top -> bottom (excluding corners)
      for (int r = 1; r < last; r++) {
        slots.add(r * 9 + 8);
      }
      // bottom row right -> left
      for (int c = 8; c >= 0; c--) {
        slots.add(last * 9 + c);
      }
      // left column bottom -> top (excluding corners)
      for (int r = last - 1; r >= 1; r--) {
        slots.add(r * 9);
      }
    }
    return slots.stream().mapToInt(Integer::intValue).toArray();
  }
}
