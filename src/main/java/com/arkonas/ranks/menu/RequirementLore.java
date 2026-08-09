package com.arkonas.ranks.menu;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import com.arkonas.ranks.messages.MessageBuilder;
import com.arkonas.ranks.placeholders.Placeholders;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

/**
 * The generated requirement block that the rank path, the prestige list and the
 * {@code {requirements}} token of a hand-written {@code lore:} all print.
 *
 * <p>Every line is a template resolved from the locale, under {@code menu.requirements}:
 *
 * <pre>
 * menu:
 *   requirements:
 *     header: ['&amp;e&amp;lREQUIREMENTS']
 *     line: '&amp;7• &amp;f{name}: &amp;b{required}'
 *     line-progress: '&amp;7• &amp;f{name}: &amp;b{current}&amp;7/&amp;b{required} &amp;7({percent}%)'
 *     types:
 *       playtime-minutes:
 *         format: time
 *         line-progress:
 *           - '&amp;7• &amp;f{name}'
 *           - '  &amp;8have: &amp;b{current}'
 *           - '  &amp;8need: &amp;f{required} &amp;7({percent}%)'
 * </pre>
 *
 * <p>Tokens are substituted textually before the Pebble/PlaceholderAPI pipeline runs, so a
 * template can mix them freely with {@code {{rank.rank}}}, {@code {{ value | money }}},
 * {@code &amp;} codes, {@code &amp;#RRGGBB} and MiniMessage. A template may be a single string
 * (split on {@code \n}) or a list, which is how one requirement prints several lines.
 *
 * <p>Nothing here is required: with no {@code menu.requirements} section at all the built-in
 * defaults render exactly what the block rendered before it was configurable.
 */
public class RequirementLore {

  /** {@code {name}: {value}} — one line per requirement, no progress. */
  static final String DEFAULT_LINE = "&7• &f{name}: &b{required}";
  /** The same line with the completion percentage appended. */
  static final String DEFAULT_PROGRESS = "&7• &f{name}: &b{required} &7({percent}%)";
  /** Requirements with nothing to count (permission, world, group, advancement...). */
  static final String DEFAULT_FLAT = "&7• &f{name}: {status}";
  static final String DEFAULT_STATUS_MET = "&a✔";
  static final String DEFAULT_STATUS_UNMET = "&c✖";
  static final String DEFAULT_TIME_FORMAT = "{days}d {hours}h {minutes}m";

  private static final List<String> NO_LINES = List.of();

  private final MenuModule module;

  public RequirementLore(MenuModule module) {
    this.module = module;
  }

  /** The requirement block for a rank, with no {@code {{next.rank}}} context. */
  public List<Component> render(Player player, Rank rank, boolean showProgress) {
    return render(player, rank, null, showProgress);
  }

  /**
   * The requirement block for a rank.
   *
   * @param rank the rank the player is leaving, whose requirements are listed
   * @param next the rank they are heading to, for {@code {{next.rank}}} in a template
   * @param showProgress whether to use the {@code line-progress} templates (the player is
   *     actually working on this rank) instead of the plain {@code line} ones
   */
  public List<Component> render(Player player, Rank rank, Rank next, boolean showProgress) {
    List<Component> lore = new ArrayList<>();
    if (rank == null) {
      return lore;
    }
    List<Requirement> requirements = order(player, rank);
    if (requirements.isEmpty()) {
      return lore;
    }

    MenuText text = module.getText();
    ConfigurationSection root = root();
    for (String line : MenuText.lines(root, "header", NO_LINES)) {
      lore.add(text.component(player, line, rank, next, null));
    }
    for (Requirement requirement : requirements) {
      State state = state(player, requirement);
      Map<String, String> tokens = tokens(requirement, state, -1);
      for (String template : template(root, requirement, state, showProgress)) {
        lore.add(text.component(player, MenuText.sub(template, tokens), rank, next,
            mb -> pebbleKeys(mb, state)));
      }
    }
    for (String line : MenuText.lines(root, "footer", NO_LINES)) {
      lore.add(text.component(player, line, rank, next, null));
    }
    return lore;
  }

  /**
   * Reads a requirement's numbers once. Callers that render the same requirement several times
   * (the rankup screen pre-builds one item per reveal step) take this once and reuse it, so a
   * plugin hook behind the requirement is not asked the same question per frame.
   */
  public Snapshot snapshot(Player player, Requirement requirement) {
    return state(player, requirement);
  }

  /**
   * The {@code {name}}, {@code {current}}, {@code {bar}} … tokens for one requirement, so the
   * icon lore on the rankup screen offers the same vocabulary as the block here.
   *
   * @param barSegments filled bar segments to draw, or -1 for the requirement's real progress
   */
  public Map<String, String> tokens(Requirement requirement, Snapshot snapshot, int barSegments) {
    return tokens(requirement, (State) snapshot, barSegments);
  }

  // --- template resolution --------------------------------------------------

  /**
   * The template for one requirement: the {@code types.<name>} scope wins over the top-level
   * one, and {@link #pick} picks the key inside a scope. Falls back to the built-in defaults,
   * so the block renders with no {@code menu.requirements} section at all.
   */
  private List<String> template(ConfigurationSection root, Requirement requirement, State state,
      boolean showProgress) {
    ConfigurationSection types = root == null ? null : root.getConfigurationSection("types");
    if (types != null) {
      for (String key : keys(requirement)) {
        if (types.isConfigurationSection(key)) {
          List<String> scoped = pick(types.getConfigurationSection(key), state, showProgress);
          if (scoped != null) {
            return scoped;
          }
        } else if (types.isSet(key)) {
          // shorthand: `types.<name>: <line>` (or a list of lines) applies to every state
          List<String> shorthand = MenuText.lines(types, key, null);
          if (shorthand != null) {
            return shorthand;
          }
        }
      }
    }
    List<String> lines = pick(root, state, showProgress);
    if (lines != null) {
      return lines;
    }
    if (!state.progressive) {
      return List.of(DEFAULT_FLAT);
    }
    return List.of(showProgress ? DEFAULT_PROGRESS : DEFAULT_LINE);
  }

  /**
   * The best-matching template key inside one scope, or null when the scope sets none. Most
   * specific first: the flat keys (for a requirement with nothing to count), then both axes
   * ({@code line-progress-unmet}), then the state, then the screen, then {@code line}.
   */
  private List<String> pick(ConfigurationSection scope, State state, boolean showProgress) {
    if (scope == null) {
      return null;
    }
    String suffix = state.met ? "-met" : "-unmet";
    if (!state.progressive) {
      List<String> flat = MenuText.lines(scope, "line-flat" + suffix,
          MenuText.lines(scope, "line-flat", null));
      if (flat != null) {
        return flat;
      }
    }
    if (showProgress) {
      List<String> both = MenuText.lines(scope, "line-progress" + suffix, null);
      if (both != null) {
        return both;
      }
    }
    List<String> stateLines = MenuText.lines(scope, "line" + suffix, null);
    if (stateLines != null) {
      return stateLines;
    }
    if (showProgress) {
      List<String> progress = MenuText.lines(scope, "line-progress", null);
      if (progress != null) {
        return progress;
      }
    }
    return MenuText.lines(scope, "line", null);
  }

  /** Lookup keys for a requirement, most specific first: {@code name#sub}, name, name minus 'h'. */
  private static List<String> keys(Requirement requirement) {
    List<String> keys = new ArrayList<>(3);
    String name = requirement.getName();
    if (requirement.hasSubRequirement()) {
      keys.add(requirement.getFullName());
    }
    if (name != null) {
      keys.add(name);
      if (name.length() > 1 && name.endsWith("h")) {
        keys.add(name.substring(0, name.length() - 1));
      }
    }
    return keys;
  }

  // --- ordering -------------------------------------------------------------

  /** The requirements to print: {@code hide-met} drops the finished ones, {@code sort} reorders. */
  private List<Requirement> order(Player player, Rank rank) {
    ConfigurationSection root = root();
    boolean hideMet = root != null && root.getBoolean("hide-met", false);
    String sort = root == null ? "config" : root.getString("sort", "config");

    List<Requirement> list = new ArrayList<>();
    for (Requirement requirement : rank.getRequirements().getRequirements(player)) {
      if (hideMet && met(player, requirement)) {
        continue;
      }
      list.add(requirement);
    }
    if ("unmet-first".equalsIgnoreCase(sort)) {
      list.sort((a, b) -> Boolean.compare(met(player, a), met(player, b)));
    } else if ("met-first".equalsIgnoreCase(sort)) {
      list.sort((a, b) -> Boolean.compare(met(player, b), met(player, a)));
    }
    return list;
  }

  // --- values ---------------------------------------------------------------

  /** What a caller may read off a {@link #snapshot(Player, Requirement)}. */
  public interface Snapshot {
    /** Whether the player already meets the requirement. */
    boolean met();

    /** The total needed, after any cost multiplier. */
    double total();

    /** Completion, 0-100. */
    int percent();
  }

  /** One requirement's numbers, read once so a template never re-hits a plugin hook per line. */
  private static final class State implements Snapshot {
    private final boolean progressive;
    private final boolean met;
    private final double total;
    private final double progress;
    private final double remaining;
    private final double fraction;
    private final int percent;

    private State(boolean progressive, boolean met, double total, double progress,
        double remaining, double fraction) {
      this.progressive = progressive;
      this.met = met;
      this.total = total;
      this.progress = progress;
      this.remaining = remaining;
      this.fraction = fraction;
      this.percent = ProgressBar.percent(fraction);
    }

    @Override
    public boolean met() {
      return met;
    }

    @Override
    public double total() {
      return total;
    }

    @Override
    public int percent() {
      return percent;
    }
  }

  private State state(Player player, Requirement requirement) {
    boolean progressive = requirement instanceof ProgressiveRequirement;
    boolean met = met(player, requirement);
    double total = requirement.getTotal(player);
    double remaining = Math.max(0, requirement.getRemaining(player));
    double progress = Math.max(0, total - remaining);
    double fraction = total <= 0 ? 1 : progress / total;
    if (!progressive) {
      // nothing to count: a flat requirement is 0% or 100%, never in between
      fraction = met ? 1 : 0;
    }
    return new State(progressive, met, total, progress, remaining, fraction);
  }

  private static boolean met(Player player, Requirement requirement) {
    try {
      return requirement.check(player);
    } catch (Throwable t) {
      return false;
    }
  }

  private void pebbleKeys(MessageBuilder mb, State state) {
    mb.replaceKey("value", state.total);
    mb.replaceKey("total", state.total);
    mb.replaceKey("current", state.progress);
    mb.replaceKey("remaining", state.remaining);
    mb.replaceKey("percent", state.percent);
  }

  private Map<String, String> tokens(Requirement requirement, State state, int barSegments) {
    String format = numberFormat(requirement);
    MenuTheme theme = module.getTheme();
    int filled = barSegments >= 0 ? barSegments : theme.progressBar().filled(state.fraction);
    double barFraction = barSegments >= 0
        ? (double) filled / theme.progressBar().length()
        : state.fraction;

    Map<String, String> tokens = new LinkedHashMap<>();
    tokens.put("name", module.getRequirementRenderer().friendlyName(requirement.getName()));
    tokens.put("type", requirement.getName() == null ? "" : requirement.getName());
    tokens.put("sub", requirement.getSub() == null ? "" : requirement.getSub());
    tokens.put("raw", requirement.getValueString() == null ? "" : requirement.getValueString());
    tokens.put("required", number(state.total, format));
    tokens.put("current", number(state.progress, format));
    tokens.put("remaining", number(state.remaining, format));
    tokens.put("percent", String.valueOf(state.percent));
    tokens.put("bar", theme.renderBar(filled, barFraction));
    tokens.put("status", state.met
        ? raw("status-met", DEFAULT_STATUS_MET)
        : raw("status-unmet", DEFAULT_STATUS_UNMET));
    tokens.put("color", "<" + (state.met ? theme.success() : theme.danger()) + ">");
    return tokens;
  }

  /**
   * The number format for a requirement: an explicit {@code types.<name>.format}, then the
   * {@code formats} map, then {@code money} for the money requirements, then {@code default}.
   */
  private String numberFormat(Requirement requirement) {
    ConfigurationSection root = root();
    ConfigurationSection types = root == null ? null : root.getConfigurationSection("types");
    if (types != null) {
      for (String key : keys(requirement)) {
        ConfigurationSection type = types.getConfigurationSection(key);
        String format = type == null ? null : type.getString("format");
        if (format != null) {
          return format;
        }
      }
    }
    ConfigurationSection formats = root == null ? null : root.getConfigurationSection("formats");
    if (formats != null) {
      for (String key : keys(requirement)) {
        String format = formats.getString(key);
        if (format != null) {
          return format;
        }
      }
    }
    if (RequirementItemRenderer.isMoney(requirement.getName())) {
      return "money";
    }
    String fallback = formats == null ? null : formats.getString("default");
    return fallback == null ? "simple" : fallback;
  }

  /** Formats one number the way {@code format} asks: the same output the Pebble filters give. */
  private String number(double value, String format) {
    Placeholders placeholders = module.getPlugin().getPlaceholders();
    String name = format == null ? "simple" : format.toLowerCase();
    switch (name) {
      case "money":
        return decimal(placeholders == null ? null : placeholders.getMoneyFormat(), value);
      case "shortmoney":
        return placeholders == null ? plain(value) : placeholders.formatMoney(value);
      case "percent":
        return decimal(placeholders == null ? null : placeholders.getPercentFormat(), value);
      case "raw":
        return plain(value);
      case "time":
      case "time-minutes":
        return time(value * 60);
      case "time-seconds":
        return time(value);
      case "time-hours":
        return time(value * 3600);
      case "time-ticks":
        return time(value / 20);
      default:
        return decimal(placeholders == null ? null : placeholders.getSimpleFormat(), value);
    }
  }

  private static String decimal(DecimalFormat format, double value) {
    return format == null ? plain(value) : format.format(value);
  }

  /** {@code 5} rather than {@code 5.0}, and the full decimal when there is one. */
  private static String plain(double value) {
    if (value == Math.rint(value) && !Double.isInfinite(value)) {
      return String.valueOf((long) value);
    }
    return String.valueOf(value);
  }

  /** Renders a duration through the {@code time-format} template. */
  private String time(double totalSeconds) {
    long seconds = (long) Math.floor(Math.max(0, totalSeconds));
    long days = seconds / 86400;
    long hours = (seconds % 86400) / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    Map<String, String> tokens = new LinkedHashMap<>();
    tokens.put("days", String.valueOf(days));
    tokens.put("hours", String.valueOf(hours));
    tokens.put("minutes", String.valueOf(minutes));
    tokens.put("seconds", String.valueOf(secs));
    tokens.put("total-hours", String.valueOf(seconds / 3600));
    tokens.put("total-minutes", String.valueOf(seconds / 60));
    tokens.put("total-seconds", String.valueOf(seconds));
    return MenuText.sub(raw("time-format", DEFAULT_TIME_FORMAT), tokens);
  }

  // --- locale access --------------------------------------------------------

  private ConfigurationSection root() {
    return module.getPlugin().getMessages().getConfigurationSection("menu.requirements");
  }

  private String raw(String key, String def) {
    return module.getText().raw("requirements." + key, def);
  }
}
