package com.arkonas.ranks.serialization;

import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PrestigeSerialized extends RankSerialized {

  private final String from;
  private final String to;

  public PrestigeSerialized(String rank, String next, String displayName,
      List<String> commands, List<String> requirements,
      Map<String, List<String>> prestigeRequirements,
      Map<String, String> messages, String from, String to) {
    super(rank, next, displayName, commands, requirements, prestigeRequirements, messages, 1.0);
    this.from = from;
    this.to = to;
  }
}
