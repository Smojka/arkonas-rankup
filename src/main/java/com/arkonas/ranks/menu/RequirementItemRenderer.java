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
    MenuIcon icon = module.getIcons().iconFor(requirement.getName());
    // one read of the requirement per item, so pre-building the fill animation does not ask a
    // plugin hook the same question once per reveal step
    RequirementLore.Snapshot snapshot = module.getRequirementLore().snapshot(player, requirement);
    boolean met = snapshot.met();
    String colour = met ? theme.success() : theme.danger();

    Component name = text.component(player,
        "<" + colour + "><bold>" + friendlyName(requirement.getName()) + "</bold>");

    // the same {name}/{current}/{required}/{percent}/{bar}/{status} vocabulary the generated
    // requirement block offers, so both surfaces are styled from one set of tokens. The
    // animated frames pass barSegments, which pins {bar} to the reveal step.
    Map<String, String> tokens =
        module.getRequirementLore().tokens(requirement, snapshot, barSegments);

    List<Component> lore = new ArrayList<>();
    if (requirement instanceof ProgressiveRequirement) {
      boolean money = isMoney(requirement.getName());
      String costMessage = money
          ? text.raw("rankup.requirement-money", "&7Cost: &6{{ value | money }}")
          : text.raw("rankup.requirement-simple", "&7Need: &f{{ value | simple }}");
      double totalValue = snapshot.total();
      lore.add(text.component(player, MenuText.sub(costMessage, tokens),
          mb -> mb.replaceKey("value", totalValue)));

      String progressMessage = MenuText.sub(
          text.raw("rankup.requirement-progress", "&b{bar} &7{percent}%"), tokens);
      lore.add(text.component(player, progressMessage));
    }

    lore.add(text.component(player, MenuText.sub(met
        ? text.raw("rankup.requirement-met", "&a✔ Requirement met")
        : text.raw("rankup.requirement-unmet", "&c✖ Not met yet"), tokens)));

    return icon.build(name, lore, met);
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
