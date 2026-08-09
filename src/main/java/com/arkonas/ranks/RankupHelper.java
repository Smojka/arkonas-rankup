package com.arkonas.ranks;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import com.arkonas.ranks.events.PlayerPrestigeEvent;
import com.arkonas.ranks.events.PlayerRankupEvent;
import com.arkonas.ranks.hook.GroupProvider;
import com.arkonas.ranks.messages.Message;
import com.arkonas.ranks.prestige.Prestige;
import com.arkonas.ranks.prestige.Prestiges;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.ranks.Rankups;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Actually performs the ranking up and prestiging for the plugin and also manages the cooldowns
 * between ranking up.
 */
public class RankupHelper {

  private final ArkonasRanksPlugin plugin;
  private final ConfigurationSection config;
  private final GroupProvider permissions;
  /**
   * Players who cannot rankup/prestige for a certain amount of time.
   */
  private final Map<java.util.UUID, Long> cooldowns = new HashMap<>();

  public RankupHelper(ArkonasRanksPlugin plugin) {
    this.plugin = plugin;
    this.config = plugin.getConfig();
    this.permissions = plugin.getPermissions();
  }

  public void doRankup(Player player, RankElement<Rank> rank) {
    if (rank.getRank() != null) {
      permissions.transferGroup(player.getUniqueId(), rank.getRank().getRank(), rank.getNext().getRank().getRank());
    } else {
      permissions.transferGroup(player.getUniqueId(), null, rank.getNext().getRank().getRank());
    }

    rank.getRank().runCommands(player, rank.getNext().getRank());

    Bukkit.getPluginManager().callEvent(new PlayerRankupEvent(plugin, player, rank));
  }

  public void sendRankupMessages(Player player, RankElement<Rank> rank) {
    plugin.getMessage(rank.getRank(), Message.SUCCESS_PUBLIC)
        .failIfEmpty()
        .replacePlayer(player)
        .replaceOldRank(rank.getRank())
        .replaceRank(rank.getNext().getRank())
        .broadcast();
    plugin.getMessage(rank.getRank(), Message.SUCCESS_PRIVATE)
        .failIfEmpty()
        .replacePlayer(player)
        .replaceOldRank(rank.getRank())
        .replaceRank(rank.getNext().getRank())
        .send(player);
  }

  public void doPrestige(Player player, RankElement<Prestige> prestige) {
    Prestige rank = prestige.getRank();

    permissions.transferGroup(player.getUniqueId(), rank.getFrom(), rank.getTo());

    permissions.transferGroup(player.getUniqueId(), rank.getRank(), prestige.getNext().getRank().getRank());

    rank.runCommands(player, prestige.getNext().getRank());

    Bukkit.getPluginManager().callEvent(new PlayerPrestigeEvent(plugin, player, prestige));
  }

  public void sendPrestigeMessages(Player player, RankElement<Prestige> prestige) {
    Objects.requireNonNull(prestige);
    Objects.requireNonNull(prestige.getNext());

    plugin.getMessage(prestige.getRank(), Message.PRESTIGE_SUCCESS_PUBLIC)
        .failIfEmpty()
        .replacePlayer(player)
        .replaceOldRank(prestige.getRank())
        .replaceRank(prestige.getNext().getRank())
        .broadcast();
    plugin.getMessage(prestige.getRank(), Message.PRESTIGE_SUCCESS_PRIVATE)
        .failIfEmpty()
        .replacePlayer(player)
        .replaceOldRank(prestige.getRank())
        .replaceRank(prestige.getNext().getRank())
        .send(player);
  }

  private boolean checkCooldown(Player player, Rank rank) {
    if (cooldowns.containsKey(player.getUniqueId())) {
      long time = System.currentTimeMillis() - cooldowns.get(player.getUniqueId());
      // if time passed is less than the cooldown
      long cooldownSeconds = config.getInt("cooldown");
      long timeLeft = (cooldownSeconds * 1000) - time;
      if (timeLeft > 0) {
        long secondsLeft = (long) Math.ceil(timeLeft / 1000f);
        plugin
            .getMessage(rank, secondsLeft > 1 ? Message.COOLDOWN_PLURAL : Message.COOLDOWN_SINGULAR)
            .failIfEmpty()
            .replacePlayer(player)
            .replaceRank(rank)
            .replaceSeconds(cooldownSeconds, secondsLeft)
            .send(player);
        return true;
      }
      // cooldown has expired so remove it
      cooldowns.remove(player.getUniqueId());
    }
    return false;
  }

  private void applyCooldown(Player player) {
    if (config.getInt("cooldown") > 0) {
      cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
  }

  /**
   * Milliseconds remaining on the rankup/prestige cooldown for a player, or 0 if
   * they are not on cooldown. Read-only: does not mutate or clear the cooldown
   * map (used by the menu module's live countdown item). Returns 0 when the
   * {@code cooldown} setting is disabled.
   *
   * @param uuid the player's unique id
   * @return non-negative milliseconds left, 0 if none
   */
  public long getCooldownRemainingMillis(java.util.UUID uuid) {
    Long start = cooldowns.get(uuid);
    if (start == null) {
      return 0;
    }
    long cooldownMillis = config.getInt("cooldown") * 1000L;
    long left = cooldownMillis - (System.currentTimeMillis() - start);
    return Math.max(0, left);
  }

  public void rankup(Player player) {
    rankup(player, plugin.getRankups());
  }

  /** Ranks a player up one step on a specific ladder, honouring the manual cooldown. */
  public void rankup(Player player, Rankups rankups) {
    if (!checkRankup(player, rankups, true)) {
      return;
    }

    RankElement<Rank> rankElement = rankups.getByPlayer(player);
    Rank rank = rankElement.getRank();
    if (!applyCost(player, rank)) {
      return;
    }
    applyCooldown(player);

    doRankup(player, rankElement);
    sendRankupMessages(player, rankElement);
  }

  /**
   * Charges a rank's costs, reporting whether they were all taken.
   *
   * <p>Every path that grants a rank must go through this and stop when it returns false. A
   * deduction can refuse (an economy rejects the withdrawal, a points plugin is down, the player's
   * balance moved between the check and the charge) and the deductions throw rather than swallow it,
   * precisely so the rank is not granted for free. This turns that into a clean refusal with a
   * message instead of an "internal error" stack trace.
   *
   * @param player the player being charged
   * @param rank the rank whose requirements are being paid
   * @return true if the costs were taken and the caller may grant the rank
   */
  public boolean applyCost(Player player, Rank rank) {
    try {
      rank.applyRequirements(player);
      return true;
    } catch (RuntimeException e) {
      plugin.getLogger().log(java.util.logging.Level.WARNING,
          "Cancelled a rankup/prestige for " + player.getName()
              + ": the cost could not be taken in full", e);
      player.sendMessage(org.bukkit.ChatColor.RED
          + "That could not be completed because the cost could not be taken."
          + " Please contact an administrator.");
      return false;
    }
  }

  /**
   * Ranks a player up a single step without applying or enforcing the manual rankup cooldown, and
   * reports whether they advanced. Used by auto-max so a pass can chain multiple rankups without
   * each step's cooldown blocking the next (the manual {@code cooldown} setting is a spam guard for
   * the {@code /rankup} command, not for automatic progression).
   *
   * @param player the player to advance
   * @return true if the player ranked up, false if they could not (not in a ladder, at the top, or
   *         requirements unmet)
   */
  public boolean rankupOnce(Player player) {
    return rankupOnce(player, plugin.getRankups());
  }

  /** Cooldown-free single-step rankup on a specific ladder; returns true if the player advanced. */
  public boolean rankupOnce(Player player, Rankups rankups) {
    if (!checkRankup(player, rankups, false)) {
      return false;
    }

    RankElement<Rank> rankElement = rankups.getByPlayer(player);
    Rank rank = rankElement.getRank();
    if (!applyCost(player, rank)) {
      return false;
    }

    doRankup(player, rankElement);
    sendRankupMessages(player, rankElement);

    // "advanced" means the player's rank actually changed, not just that the transfer was attempted.
    // Callers loop on this (auto-max, /maxrankup); if the group change is a no-op — permission-rankup
    // mode, or a permission backend that silently failed — reporting true would charge the player for
    // the same rank on every pass until the iteration cap.
    if (rankups.getByPlayer(player) == rankElement) {
      // in permission-rankup mode the rank is granted by the rank's own commands, which a permission
      // plugin may only apply on the next tick, so a same-tick stall there is expected, not a fault
      if (!config.getBoolean("permission-rankup")) {
        warnNoProgressOnce(player, rank);
      }
      return false;
    }
    return true;
  }

  /** Players already warned about a stalled rank change, so a broken setup logs once, not per tick. */
  private final java.util.Set<java.util.UUID> warnedNoProgress = new java.util.HashSet<>();

  private void warnNoProgressOnce(Player player, Rank rank) {
    if (warnedNoProgress.add(player.getUniqueId())) {
      plugin.getLogger().warning(player.getName() + " met the requirements for a rankup from '"
          + rank.getRank() + "' but their rank did not change afterwards. Automatic progression is"
          + " stopped for them so the same rank is not charged again. Check that the permission"
          + " plugin is applying group changes.");
    }
  }

  public boolean checkRankup(Player player) {
    return checkRankup(player, plugin.getRankups(), true);
  }

  public boolean checkRankup(Player player, boolean message) {
    return checkRankup(player, plugin.getRankups(), message);
  }

  /**
   * Checks if a player can rankup on a specific ladder, and if they can't, sends the player a
   * message and returns false. The prestige-hint branch (MUST_PRESTIGE) only applies to the default
   * ladder, since prestige is a single global track.
   *
   * @param player the player to check
   * @param rankups the ladder to check against
   * @param message whether to send feedback messages (and enforce the cooldown)
   * @return true if the player can rankup, false otherwise
   */
  public boolean checkRankup(Player player, Rankups rankups, boolean message) {
    RankElement<Rank> rankElement = rankups.getByPlayer(player);
    if (rankElement == null) { // check if in ladder
      plugin.getMessage(Message.NOT_IN_LADDER)
          .failIf(!message)
          .replacePlayer(player)
          .send(player);
      return false;
    }
    Rank rank = rankElement.getRank();
    if (!rankElement.hasNext()) {
      Message pMessage = Message.NO_RANKUP;
      // prestige is a single global track, only hinted at from the default ladder
      if (rankups == plugin.getRankups()) {
        Prestiges prestiges = plugin.getPrestiges();
        if (prestiges != null) {
          RankElement<Prestige> byPlayer = prestiges.getByPlayer(player);
          if (byPlayer != null && byPlayer.hasNext()) {
            pMessage = Message.MUST_PRESTIGE;
          }
        }
      }
      plugin.getMessage(pMessage)
          .failIf(!message)
          .replacePlayer(player)
          .replaceRank(rankups.getTree().last().getRank())
          .send(player);
      return false;
    } else if (!rank.hasRequirements(player)) { // check if they can afford it
      if (message) {
        plugin.getMessage(rank, Message.REQUIREMENTS_NOT_MET)
            .replacePlayer(player)
            .replaceOldRank(rank)
            .replaceRank(rankElement.getNext().getRank())
            .send(player);
      }
      return false;
    } else if (message && checkCooldown(player, rank)) {
      return false;
    }

    return true;
  }

  public void prestige(Player player) {
    if (!checkPrestige(player)) {
      return;
    }

    RankElement<Prestige> rankElement = plugin.getPrestiges().getByPlayer(player);
    Prestige prestige = rankElement.getRank();
    if (!applyCost(player, prestige)) {
      return;
    }

    applyCooldown(player);
    doPrestige(player, rankElement);
    sendPrestigeMessages(player, rankElement);
  }

  public boolean checkPrestige(Player player) {
    return checkPrestige(player, true);
  }

  public boolean checkPrestige(Player player, boolean message) {
    Prestiges prestiges = plugin.getPrestiges();
    RankElement<Prestige> prestigeElement = prestiges.getByPlayer(player);
    if (prestigeElement == null
        || !prestigeElement.getRank().isEligible(player)) { // check if in ladder
      plugin.getMessage(Message.NOT_HIGH_ENOUGH)
          .failIf(!message)
          .replacePlayer(player)
          .send(player);
      return false;
    } else if (!prestigeElement.hasNext()) { // check if they are at the highest rank
      plugin.getMessage(prestigeElement.getRank(), Message.PRESTIGE_NO_PRESTIGE)
          .failIf(!message)
          .replacePlayer(player)
          .replaceRank(prestigeElement.getRank())
          .send(player);
      return false;
    } else if (!prestigeElement.getRank().hasRequirements(player)) { // check if they can afford it
      plugin.getMessage(prestigeElement.getRank(), Message.PRESTIGE_REQUIREMENTS_NOT_MET)
          .failIf(!message)
          .replacePlayer(player)
          .replaceOldRank(prestigeElement.getRank())
          .replaceRank(prestigeElement.getNext().getRank())
          .send(player);
      return false;
    } else if (checkCooldown(player, prestigeElement.getRank())) {
      return false;
    }

    return true;
  }
}
