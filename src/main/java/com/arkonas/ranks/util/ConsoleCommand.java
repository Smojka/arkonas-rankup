package com.arkonas.ranks.util;

import java.util.logging.Logger;
import org.bukkit.Bukkit;

/**
 * Dispatches a configured reward command from the console after sanitising it.
 *
 * <p>Rank, prestige and rebirth commands are rendered through the message pipeline, which resolves
 * PlaceholderAPI placeholders. Those can expand to text a player controls (a nickname, a display
 * name, a town name), so the rendered string is not fully trusted even though the template is. This
 * strips the parts of that string that can change how the command is read — control characters,
 * which several command routers treat as separators, and a leading slash — and caps the length so a
 * runaway placeholder cannot push a multi-megabyte line into the command map.
 *
 * <p>It cannot make argument splicing impossible: a command template such as
 * {@code eco give {player} 100} run for a player whose <em>rendered</em> name contains spaces will
 * always produce extra arguments. Keep player-controlled placeholders out of command templates and
 * prefer {@code {player}} (the account name, always {@code [A-Za-z0-9_]}) over display names.
 */
public final class ConsoleCommand {

  /** Longest command accepted; anything past this is a rendering accident, not a reward. */
  private static final int MAX_LENGTH = 2048;

  private ConsoleCommand() {
  }

  /**
   * Dispatches {@code rendered} from the console, or does nothing when it is blank.
   *
   * @param logger the plugin logger, used to report a rejected command
   * @param rendered the fully rendered command line
   */
  public static void dispatch(Logger logger, String rendered) {
    String command = sanitise(logger, rendered);
    if (command == null) {
      return;
    }
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
  }

  /**
   * The dispatchable form of a rendered command, or null when it should not run.
   *
   * @param logger the plugin logger, used to report a rejected command
   * @param rendered the fully rendered command line
   * @return the command without its leading slash and control characters, or null
   */
  public static String sanitise(Logger logger, String rendered) {
    if (rendered == null) {
      return null;
    }

    StringBuilder cleaned = new StringBuilder(rendered.length());
    boolean stripped = false;
    for (int i = 0; i < rendered.length(); i++) {
      char c = rendered.charAt(i);
      // \n and \r end a command for several routers, and NUL/other C0 controls are never meaningful
      // in a command line; a placeholder that produced them is either broken or hostile.
      // Replaced with a space rather than dropped, so the text either side of one cannot fuse into a
      // single token that means something else
      if (c == '\n' || c == '\r' || c < ' ' || c == '\u007F') {
        stripped = true;
        cleaned.append(' ');
        continue;
      }
      cleaned.append(c);
    }

    String command = cleaned.toString().trim();
    while (command.startsWith("/")) {
      command = command.substring(1).trim();
    }
    if (command.isBlank()) {
      return null;
    }
    if (command.length() > MAX_LENGTH) {
      logger.warning("Refusing to run a reward command longer than " + MAX_LENGTH
          + " characters; check the placeholders in it: " + command.substring(0, 120) + "...");
      return null;
    }
    if (stripped) {
      logger.warning("Stripped control characters from a reward command before running it: "
          + command);
    }
    return command;
  }
}
