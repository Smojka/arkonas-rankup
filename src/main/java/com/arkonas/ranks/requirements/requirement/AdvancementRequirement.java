package com.arkonas.ranks.requirements.requirement;

import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.requirements.Requirement;

import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Advancement requirement
 * @author Link, with modifications from Okx
 */
public class AdvancementRequirement extends Requirement {
  public AdvancementRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "advancement");
  }

  protected AdvancementRequirement(Requirement clone) {
    super(clone);
  }

  @Override
  public boolean check(Player player) {
    for (String string : getValuesString()) {
      Iterator<Advancement> advancementIterator = Bukkit.advancementIterator();
      while (advancementIterator.hasNext()) {
        Advancement adv = advancementIterator.next();
        String key = adv.getKey().getKey();
        Pattern pattern = Pattern.compile(string.replace("*", ".*").replace("-", ""));
        if (pattern.matcher(key).find()) {
          boolean positive = !string.startsWith("-");

          AdvancementProgress progress = player.getAdvancementProgress(adv);
          if (progress.isDone() == positive) {
            return true;
          }
        }


      }
    }
    return false;
  }

  @Override
  public Requirement clone() {
    return new AdvancementRequirement(this);
  }

  @Override
  public double getValueDouble() {
    return 1;
  }
}
