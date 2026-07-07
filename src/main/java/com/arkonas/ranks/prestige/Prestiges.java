package com.arkonas.ranks.prestige;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import com.arkonas.ranks.ArkonasRanksPlugin;
import com.arkonas.ranks.ranks.RankElement;
import com.arkonas.ranks.ranks.RankList;

public class Prestiges extends RankList<Prestige> {
  public Prestiges(ArkonasRanksPlugin plugin, FileConfiguration config) {
    super(plugin, convert(plugin, config));
  }

  private static List<Prestige> convert(ArkonasRanksPlugin plugin, FileConfiguration config) {
    Map<String, Object> values = config.getValues(false);
    List<Prestige> prestiges = new ArrayList<>(values.size());
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      prestiges.add(Prestige.deserialize(plugin, (ConfigurationSection) entry.getValue()));
    }
    return prestiges;
  }

  @Override
  protected void addLastRank(ArkonasRanksPlugin plugin) {
    RankElement<Prestige> last = getTree().last();
    last.setNext(new RankElement<>(new LastPrestige(plugin, last.getRank().getNext()), null));
  }

}
