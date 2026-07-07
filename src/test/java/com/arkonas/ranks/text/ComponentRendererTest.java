package com.arkonas.ranks.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

public class ComponentRendererTest {

  private final ComponentRenderer auto = ComponentRenderer.of("auto");

  private static String plain(Component component) {
    return PlainTextComponentSerializer.plainText().serialize(component);
  }

  @Test
  public void testLegacyCodesTranslate() {
    assertEquals("<gold><b>Hi <red>there", MixedComponentRenderer.toMiniMessage("&6&lHi &cthere"));
  }

  @Test
  public void testAmpersandHex() {
    assertEquals("<#FF0000>red", MixedComponentRenderer.toMiniMessage("&#FF0000red"));
  }

  @Test
  public void testBungeeHex() {
    assertEquals("<#FF0000>red", MixedComponentRenderer.toMiniMessage("§x§F§F§0§0§0§0red"));
  }

  @Test
  public void testPureLegacyKeepsRankupShape() {
    Component expected = new LegacyComponentRenderer().render("&7A &8» &7B");
    assertEquals(expected, auto.render("&7A &8» &7B"));
  }

  @Test
  public void testMixedLegacyAndMiniMessage() {
    Component rendered = auto.render("&6gold <red>red</red> &#00FF00hex");
    assertEquals("gold red hex", plain(rendered));
  }

  @Test
  public void testGradientTagRenders() {
    // gradients produce virtual components without stable equality; compare serialized forms
    String expected = MiniMessage.miniMessage()
        .serialize(MiniMessage.miniMessage().deserialize("<gradient:red:blue>hello</gradient>"));
    String actual = MiniMessage.miniMessage()
        .serialize(auto.render("<gradient:red:blue>hello</gradient>"));
    assertEquals(expected, actual);
  }

  @Test
  public void testLiteralAngleBracketSurvives() {
    assertEquals("i <3 u", plain(auto.render("i <3 u")));
  }

  @Test
  public void testMiniMessageOnlyMode() {
    ComponentRenderer mm = ComponentRenderer.of("minimessage");
    assertEquals("hi", plain(mm.render("<yellow>hi</yellow>")));
  }
}
