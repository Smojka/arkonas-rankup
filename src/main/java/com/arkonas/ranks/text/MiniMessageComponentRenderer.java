package com.arkonas.ranks.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Pure MiniMessage rendering; legacy codes are not translated.
 */
public class MiniMessageComponentRenderer implements ComponentRenderer {

  private static final MiniMessage MINI = MiniMessage.miniMessage();

  @Override
  public Component render(String message) {
    return MINI.deserialize(message);
  }
}
