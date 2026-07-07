package com.arkonas.ranks.menu;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import com.arkonas.ranks.ArkonasRanksPlugin;

/**
 * The single repeating task that animates every open menu. Lazily started when
 * the first menu opens and cancelled as soon as no menus remain (or on
 * reload/disable). Increments a global frame counter and calls
 * {@link AbstractMenu#tick(long)} on each open menu, defensively cleaning up any
 * menu whose player has gone offline.
 */
public class MenuTicker {

  private final ArkonasRanksPlugin plugin;
  private final MenuModule module;
  private final long period;

  private BukkitTask task;
  private long frame;

  public MenuTicker(ArkonasRanksPlugin plugin, MenuModule module, long period) {
    this.plugin = plugin;
    this.module = module;
    this.period = period;
  }

  public long frame() {
    return frame;
  }

  /** Starts the task if it is not already running. */
  public void ensureRunning() {
    if (task == null) {
      task = Bukkit.getScheduler().runTaskTimer(plugin, this::run, period, period);
    }
  }

  /** Cancels the task (safe to call when not running). */
  public void stop() {
    if (task != null) {
      try {
        task.cancel();
      } catch (Exception ignored) {
        // already cancelled
      }
      task = null;
    }
  }

  private void run() {
    if (module.getOpenMenus().isEmpty()) {
      stop();
      return;
    }
    frame++;
    for (AbstractMenu menu : new ArrayList<>(module.getOpenMenus())) {
      Player player = menu.getPlayer();
      if (player == null || !player.isOnline()) {
        module.forget(menu);
        continue;
      }
      try {
        menu.tick(frame);
      } catch (Exception e) {
        plugin.getLogger().warning("Menu tick failed: " + e.getMessage());
      }
    }
    if (module.getOpenMenus().isEmpty()) {
      stop();
    }
  }
}
