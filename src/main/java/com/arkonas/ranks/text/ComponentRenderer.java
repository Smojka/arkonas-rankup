package com.arkonas.ranks.text;

import net.kyori.adventure.text.Component;

/**
 * Renders a fully processed message string (after the legacy/PAPI/Pebble stages)
 * into an Adventure {@link Component} for sending to players.
 */
public interface ComponentRenderer {

  Component render(String message);

  /**
   * @param format one of "auto", "legacy", "minimessage" (config key message-format)
   */
  static ComponentRenderer of(String format) {
    return switch (format == null ? "auto" : format.toLowerCase()) {
      case "legacy" -> new LegacyComponentRenderer();
      case "minimessage" -> new MiniMessageComponentRenderer();
      default -> new MixedComponentRenderer();
    };
  }
}
