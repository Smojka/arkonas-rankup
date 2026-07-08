package com.arkonas.ranks;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically advances players who hold {@code rankup.auto}. Behaviour is controlled by the
 * optional {@code auto} config section:
 * <ul>
 *   <li>{@code auto.rankup} (default true) — auto rank up</li>
 *   <li>{@code auto.prestige} (default true) — auto prestige when at the top rank</li>
 *   <li>{@code auto.max} (default false) — rank up repeatedly in one pass until unaffordable</li>
 * </ul>
 * When the {@code auto} section is absent the legacy behaviour is preserved exactly: rank up once,
 * otherwise prestige. The task is only scheduled when {@code autorankup-interval > 0}.
 */
@RequiredArgsConstructor
public class AutoRankup extends BukkitRunnable {

  /** Safety bound on a single max-rankup pass, guarding against a mis-configured rank loop. */
  private static final int MAX_ITERATIONS = 1000;

  private final ArkonasRanksPlugin rankup;

  @Override
  public void run() {
    if (rankup.error()) {
      return;
    }

    ConfigurationSection auto = rankup.getConfig().getConfigurationSection("auto");
    boolean doRankup = auto == null || auto.getBoolean("rankup", true);
    boolean doPrestige = auto == null || auto.getBoolean("prestige", true);
    boolean doMax = auto != null && auto.getBoolean("max", false);

    RankupHelper helper = rankup.getHelper();
    java.util.Collection<com.arkonas.ranks.ranks.Rankups> ladders =
        rankup.getLadders() != null ? rankup.getLadders().all()
            : java.util.Collections.singletonList(rankup.getRankups());

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.hasPermission("rankup.auto")) {
        continue;
      }

      boolean rankedThisPass = false;
      if (doRankup) {
        // each ladder is progressed independently
        for (com.arkonas.ranks.ranks.Rankups ladder : ladders) {
          if (doMax) {
            int guard = 0;
            while (helper.rankupOnce(player, ladder)) {
              rankedThisPass = true;
              if (++guard >= MAX_ITERATIONS) {
                break;
              }
            }
          } else if (helper.checkRankup(player, ladder, false)) {
            helper.rankup(player, ladder);
            rankedThisPass = true;
          }
        }
      }

      // prestige is a single global track, checked once when no ladder advanced
      if (!rankedThisPass && doPrestige && rankup.getPrestiges() != null
          && helper.checkPrestige(player, false)) {
        helper.prestige(player);
      }
    }
  }
}
