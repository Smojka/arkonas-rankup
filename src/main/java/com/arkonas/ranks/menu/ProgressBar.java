package com.arkonas.ranks.menu;

/**
 * Pure utility for rendering a unicode progress bar such as {@code ▰▰▰▱▱}.
 * Stateless and allocation-light so it can be called while pre-building the
 * animation lore variants (never inside a tick).
 */
public final class ProgressBar {

  private final String filledChar;
  private final String emptyChar;
  private final int length;

  public ProgressBar(String filledChar, String emptyChar, int length) {
    this.filledChar = filledChar;
    this.emptyChar = emptyChar;
    this.length = Math.max(1, length);
  }

  public int length() {
    return length;
  }

  /**
   * Renders a bar for a fraction in {@code [0, 1]}.
   *
   * @param fraction progress fraction, clamped to [0, 1]
   * @return a bar string with {@link #length} segments
   */
  public String bar(double fraction) {
    return partialBar(filled(fraction));
  }

  /**
   * Renders a bar with an exact number of filled segments. Used by the fill
   * animation, which reveals one segment per frame.
   *
   * @param filledSegments how many segments are filled, clamped to [0, length]
   * @return a bar string
   */
  public String partialBar(int filledSegments) {
    int filled = Math.max(0, Math.min(length, filledSegments));
    StringBuilder sb = new StringBuilder(length * filledChar.length());
    for (int i = 0; i < length; i++) {
      sb.append(i < filled ? filledChar : emptyChar);
    }
    return sb.toString();
  }

  /**
   * @param fraction progress fraction
   * @return number of filled segments for that fraction
   */
  public int filled(double fraction) {
    double clamped = Math.max(0, Math.min(1, fraction));
    return (int) Math.round(clamped * length);
  }

  /**
   * @param fraction progress fraction
   * @return integer percentage 0-100
   */
  public static int percent(double fraction) {
    return (int) Math.round(Math.max(0, Math.min(1, fraction)) * 100);
  }

  /** Eighth-block glyphs from empty to full, for sub-segment smoothing. */
  private static final char[] EIGHTHS = {' ', '▏', '▎', '▍', '▌', '▋', '▊', '▉', '█'};

  /**
   * Renders a smooth bar where the boundary segment is drawn with a partial eighth-block glyph, so
   * progress advances a fraction of a cell at a time rather than jumping a whole segment. Uses the
   * eighth-block characters and ignores the configured filled/empty chars.
   *
   * @param fraction progress fraction, clamped to [0, 1]
   * @return a length-{@link #length} string of block glyphs
   */
  public String smoothBar(double fraction) {
    double clamped = Math.max(0, Math.min(1, fraction));
    double totalEighths = clamped * length * 8.0;
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      double cellEighths = totalEighths - (i * 8.0);
      int eighths = (int) Math.max(0, Math.min(8, Math.round(cellEighths)));
      sb.append(EIGHTHS[eighths]);
    }
    return sb.toString();
  }

  /**
   * Wraps a bar in a MiniMessage hex gradient over the filled portion, with the empty portion in a
   * dim colour. The result is a MiniMessage string to be rendered by the menu text pipeline.
   *
   * @param fraction progress fraction, clamped to [0, 1]
   * @param fromHex start colour, e.g. {@code #55ff55} or {@code 55ff55}
   * @param toHex end colour
   * @param emptyHex colour for the empty portion
   * @return a MiniMessage string
   */
  public String gradientBar(double fraction, String fromHex, String toHex, String emptyHex) {
    int filled = filled(fraction);
    StringBuilder filledPart = new StringBuilder();
    StringBuilder emptyPart = new StringBuilder();
    for (int i = 0; i < length; i++) {
      if (i < filled) {
        filledPart.append(filledChar);
      } else {
        emptyPart.append(emptyChar);
      }
    }
    StringBuilder out = new StringBuilder();
    if (filled > 0) {
      out.append("<gradient:").append(hex(fromHex)).append(':').append(hex(toHex)).append('>')
          .append(filledPart).append("</gradient>");
    }
    if (filled < length) {
      out.append('<').append(hex(emptyHex)).append('>').append(emptyPart);
    }
    return out.toString();
  }

  private static String hex(String colour) {
    if (colour == null) {
      return "#ffffff";
    }
    return colour.startsWith("#") ? colour : "#" + colour;
  }
}
