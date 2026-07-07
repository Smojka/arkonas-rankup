package com.arkonas.ranks.text;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Default renderer: one message may mix legacy &amp; codes, &amp;#RRGGBB / §x hex and
 * MiniMessage tags. Legacy codes are rewritten to their MiniMessage equivalents,
 * then the whole string is parsed leniently by MiniMessage.
 */
public class MixedComponentRenderer implements ComponentRenderer {

  private static final MiniMessage MINI = MiniMessage.miniMessage();

  private static final Map<Character, String> TAGS = Map.ofEntries(
      Map.entry('0', "<black>"), Map.entry('1', "<dark_blue>"), Map.entry('2', "<dark_green>"),
      Map.entry('3', "<dark_aqua>"), Map.entry('4', "<dark_red>"), Map.entry('5', "<dark_purple>"),
      Map.entry('6', "<gold>"), Map.entry('7', "<gray>"), Map.entry('8', "<dark_gray>"),
      Map.entry('9', "<blue>"), Map.entry('a', "<green>"), Map.entry('b', "<aqua>"),
      Map.entry('c', "<red>"), Map.entry('d', "<light_purple>"), Map.entry('e', "<yellow>"),
      Map.entry('f', "<white>"), Map.entry('k', "<obf>"), Map.entry('l', "<b>"),
      Map.entry('m', "<st>"), Map.entry('n', "<u>"), Map.entry('o', "<i>"),
      Map.entry('r', "<reset>"));

  // &#RRGGBB (Rankup3 style) — also matches with § in place of &
  private static final Pattern AMP_HEX = Pattern.compile("[&§]#([0-9a-fA-F]{6})");
  // §x§R§R§G§G§B§B (Bungee style, produced by PAPI expansions)
  private static final Pattern X_HEX = Pattern.compile(
      "[&§][xX]([&§][0-9a-fA-F]){6}");
  private static final Pattern CODE = Pattern.compile("[&§]([0-9a-fA-Fk-oK-OrR])");

  // looks like the start of a MiniMessage tag, e.g. <gold>, </bold>, <#ff0000>, <gradient:...
  private static final Pattern MINI_TAG = Pattern.compile("<[a-zA-Z#/!?]");

  private static final LegacyComponentRenderer LEGACY = new LegacyComponentRenderer();

  @Override
  public Component render(String message) {
    // pure legacy messages keep the exact Rankup3 component shape;
    // MiniMessage parsing only happens when a tag is actually present
    if (!MINI_TAG.matcher(message).find()) {
      return LEGACY.render(message);
    }
    return MINI.deserialize(toMiniMessage(message));
  }

  static String toMiniMessage(String message) {
    Matcher xHex = X_HEX.matcher(message);
    StringBuilder sb = new StringBuilder();
    while (xHex.find()) {
      String raw = xHex.group().replaceAll("[&§xX]", "");
      xHex.appendReplacement(sb, "<#" + raw + ">");
    }
    xHex.appendTail(sb);
    message = sb.toString();

    message = AMP_HEX.matcher(message).replaceAll("<#$1>");

    Matcher code = CODE.matcher(message);
    sb = new StringBuilder();
    while (code.find()) {
      String tag = TAGS.get(Character.toLowerCase(code.group(1).charAt(0)));
      code.appendReplacement(sb, Matcher.quoteReplacement(tag));
    }
    code.appendTail(sb);
    return sb.toString();
  }
}
