package com.arkonas.ranks.text;

import com.arkonas.ranks.util.Colour;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Legacy rendering: only &amp; codes and &amp;#RRGGBB hex, no MiniMessage.
 */
public class LegacyComponentRenderer implements ComponentRenderer {

  private static final LegacyComponentSerializer SERIALIZER =
      LegacyComponentSerializer.builder().character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

  @Override
  public Component render(String message) {
    return SERIALIZER.deserialize(Colour.translate(message));
  }
}
