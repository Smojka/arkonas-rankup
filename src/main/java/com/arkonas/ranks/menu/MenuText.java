package com.arkonas.ranks.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.messages.MessageBuilder;
import com.arkonas.ranks.ranks.Rank;

/**
 * Resolves {@code menu.*} locale strings and renders them into Adventure
 * {@link Component}s through the plugin's Pebble/PAPI text pipeline
 * (see {@link MessageBuilder#toComponent(Player)}), always disabling the
 * default italic decoration of item names/lore.
 *
 * <p>Menu-only tokens like {@code {bar}}, {@code {percent}} and {@code {seconds}}
 * are substituted textually <em>before</em> the pipeline runs, matching the
 * plan's "{X} substitution ahead of the pipeline" rule.
 */
public class MenuText {

  private static final Pattern COLOUR_CODES =
      Pattern.compile("[§&](#[0-9a-fA-F]{6}|[0-9a-fk-orA-FK-OR])");

  private final ArkonasRanksPlugin plugin;
  private final MenuTheme theme;

  public MenuText(ArkonasRanksPlugin plugin, MenuTheme theme) {
    this.plugin = plugin;
    this.theme = theme;
  }

  /** Raw locale string for {@code menu.<path>}, or {@code def} when absent. */
  public String raw(String path, String def) {
    String value = plugin.getMessages().getString("menu." + path);
    return value == null ? def : value;
  }

  /**
   * Raw locale lines for {@code menu.<path>}: a YAML list as-is, a single string split on
   * {@code \n}, or {@code def} when the key is absent or empty.
   */
  public List<String> rawLines(String path, List<String> def) {
    return lines(plugin.getMessages(), "menu." + path, def);
  }

  /**
   * One lore key off a config section, in either shape: a list of lines, or a single string
   * split on {@code \n}. Returns {@code def} when the key is unset or resolves to nothing.
   */
  public static List<String> lines(ConfigurationSection section, String path, List<String> def) {
    if (section == null || !section.isSet(path)) {
      return def;
    }
    if (section.isList(path)) {
      List<String> list = section.getStringList(path);
      return list.isEmpty() ? def : list;
    }
    String single = section.getString(path);
    if (single == null || single.isEmpty()) {
      return def;
    }
    return List.of(single.split("\n", -1));
  }

  /** Substitutes {@code {key}} tokens literally, before Pebble runs. */
  public static String sub(String message, Map<String, String> tokens) {
    String result = message;
    for (Map.Entry<String, String> entry : tokens.entrySet()) {
      result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return result;
  }

  public Component component(Player player, String message) {
    return component(player, message, null, null);
  }

  public Component component(Player player, String message, Rank oldRank, Rank next) {
    return builder(player, message, oldRank, next).toComponent(player)
        .decoration(TextDecoration.ITALIC, false);
  }

  /** Renders a component with arbitrary extra context set on the builder. */
  public Component component(Player player, String message, Consumer<MessageBuilder> setup) {
    MessageBuilder mb = plugin.newMessageBuilder(message).replacePlayer(player);
    setup.accept(mb);
    return mb.toComponent(player).decoration(TextDecoration.ITALIC, false);
  }

  /**
   * Renders a component with both rank context ({@code {{rank.rank}}}, {@code {{next.rank}}})
   * and arbitrary extra keys; {@code setup} may be null.
   */
  public Component component(Player player, String message, Rank oldRank, Rank next,
      Consumer<MessageBuilder> setup) {
    MessageBuilder mb = builder(player, message, oldRank, next);
    if (setup != null) {
      setup.accept(mb);
    }
    return mb.toComponent(player).decoration(TextDecoration.ITALIC, false);
  }

  /** Splits a message on {@code \n} and renders each line as a lore component. */
  public List<Component> lore(Player player, String message, Rank oldRank, Rank next) {
    List<Component> lore = new ArrayList<>();
    for (String line : message.split("\n", -1)) {
      lore.add(builder(player, line, oldRank, next).toComponent(player)
          .decoration(TextDecoration.ITALIC, false));
    }
    return lore;
  }

  /**
   * Renders a menu title. The message is resolved through the pipeline to plain
   * text (Pebble tokens like {@code {{next.rank}}} are expanded), stripped of
   * legacy colour codes, then wrapped in the theme gradient.
   */
  public Component title(Player player, String message, Rank oldRank, Rank next) {
    String plain = COLOUR_CODES.matcher(
        builder(player, message, oldRank, next).toString(player)).replaceAll("");
    return theme.gradientTitle(plain);
  }

  private MessageBuilder builder(Player player, String message, Rank oldRank, Rank next) {
    MessageBuilder mb = plugin.newMessageBuilder(message).replacePlayer(player);
    if (oldRank != null) {
      mb = mb.replaceOldRank(oldRank);
    }
    if (next != null) {
      mb = mb.replaceRank(next);
    }
    return mb;
  }
}
