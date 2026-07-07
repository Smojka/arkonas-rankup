package com.arkonas.ranks.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.arkonas.ranks.requirements.ProgressiveRequirement;
import com.arkonas.ranks.requirements.Requirement;

/**
 * Renders a single {@link Requirement} into a menu {@link ItemStack}: the icon
 * material comes from {@link RequirementIcons}, the colour reflects met/unmet,
 * progressive requirements get a unicode progress bar (money via the Pebble
 * {@code money} filter, others via {@code simple}), and non-progressive ones get
 * a plain ✔/✖ status line.
 */
public class RequirementItemRenderer {

  private final MenuModule module;

  public RequirementItemRenderer(MenuModule module) {
    this.module = module;
  }

  public ItemStack render(Player player, Requirement requirement) {
    return render(player, requirement, -1);
  }

  /**
   * Renders the requirement icon. When {@code barSegments >= 0} the progress bar
   * is drawn with that many filled segments instead of the actual progress; this
   * is used by the fill animation, which pre-builds one item per reveal step so
   * the ticker never runs Pebble. All other lore (cost, met/unmet) reflects the
   * real state.
   */
  public ItemStack render(Player player, Requirement requirement, int barSegments) {
    MenuTheme theme = module.getTheme();
    MenuText text = module.getText();
    Material material = module.getIcons().forRequirement(requirement.getName());
    boolean met = requirement.check(player);
    String colour = met ? theme.success() : theme.danger();

    Component name = text.component(player,
        "<" + colour + "><bold>" + friendlyName(requirement.getName()) + "</bold>");

    List<Component> lore = new ArrayList<>();
    if (requirement instanceof ProgressiveRequirement) {
      double total = requirement.getTotal(player);
      double remaining = requirement.getRemaining(player);
      double progress = Math.max(0, total - remaining);
      double fraction = total <= 0 ? 1 : progress / total;
      int filled = barSegments >= 0 ? barSegments : theme.progressBar().filled(fraction);
      String bar = theme.progressBar().partialBar(filled);
      int percent = ProgressBar.percent(fraction);

      boolean money = isMoney(requirement.getName());
      String costMessage = money
          ? text.raw("rankup.requirement-money", "&7Cost: &6{{ value | money }}")
          : text.raw("rankup.requirement-simple", "&7Need: &f{{ value | simple }}");
      double totalValue = total;
      lore.add(text.component(player, costMessage, mb -> mb.replaceKey("value", totalValue)));

      String progressMessage = MenuText.sub(
          text.raw("rankup.requirement-progress", "&b{bar} &7{percent}%"),
          Map.of("bar", bar, "percent", String.valueOf(percent)));
      lore.add(text.component(player, progressMessage));
    }

    lore.add(text.component(player, met
        ? text.raw("rankup.requirement-met", "&a✔ Requirement met")
        : text.raw("rankup.requirement-unmet", "&c✖ Not met yet")));

    return MenuItems.build(material, name, lore, met);
  }

  /** The number of filled bar segments for a requirement's actual progress. */
  public int targetSegments(Player player, Requirement requirement) {
    if (!(requirement instanceof ProgressiveRequirement)) {
      return 0;
    }
    double total = requirement.getTotal(player);
    double remaining = requirement.getRemaining(player);
    double fraction = total <= 0 ? 1 : Math.max(0, total - remaining) / total;
    return module.getTheme().progressBar().filled(fraction);
  }

  public static boolean isMoney(String name) {
    if (name == null) {
      return false;
    }
    String lower = name.toLowerCase();
    return lower.equals("money") || lower.equals("moneyh");
  }

  public String friendlyName(String name) {
    String override = module.getPlugin().getMessages()
        .getString("menu.rankup.requirement-names." + name);
    if (override != null) {
      return override;
    }
    if (name == null || name.isEmpty()) {
      return "Requirement";
    }
    String cleaned = name.replace('-', ' ').replace('_', ' ');
    if (cleaned.endsWith("h") && cleaned.length() > 1) {
      cleaned = cleaned.substring(0, cleaned.length() - 1);
    }
    String[] words = cleaned.split(" ");
    StringBuilder sb = new StringBuilder();
    for (String word : words) {
      if (word.isEmpty()) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }
    return sb.toString();
  }
}
