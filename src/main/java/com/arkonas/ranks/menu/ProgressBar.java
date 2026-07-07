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
}
