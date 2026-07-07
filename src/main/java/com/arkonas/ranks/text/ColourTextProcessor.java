package com.arkonas.ranks.text;

import com.arkonas.ranks.util.Colour;

public class ColourTextProcessor implements TextProcessor {

  @Override
  public String process(String string) {
    return Colour.translate(string);
  }
}
