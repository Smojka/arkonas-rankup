package com.arkonas.ranks.text.pebble;

import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import java.util.List;
import java.util.Map;
import com.arkonas.ranks.placeholders.Placeholders;

public class MoneyShortFilter implements Filter {

  private final Placeholders placeholders;

  public MoneyShortFilter(Placeholders placeholders) {
    this.placeholders = placeholders;
  }

  @Override
  public List<String> getArgumentNames() {
    return null;
  }

  @Override
  public Object apply(Object input, Map<String, Object> args, PebbleTemplate self,
      EvaluationContext context, int lineNumber) throws PebbleException {
    if (input == null) {
      return null;
    }
    if (!(input instanceof Number)) {
      throw new PebbleException(null, "The input for the 'MoneyShortFilter' filter has to be a number: " + input,
          lineNumber, self.getName());
    }

    Number number = (Number) input;
    return placeholders.formatMoney(number.doubleValue());
  }
}
