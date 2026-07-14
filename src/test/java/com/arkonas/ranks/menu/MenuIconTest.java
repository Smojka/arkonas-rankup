package com.arkonas.ranks.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Modern menu-icon building: material, custom-model-data, glow, and config parsing. */
class MenuIconTest {

  @BeforeEach
  void setup() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void plainMaterialIcon() {
    ItemStack item = MenuIcon.of(Material.DIAMOND).build(null, null, false);
    assertEquals(Material.DIAMOND, item.getType());
    assertFalse(item.getItemMeta().hasCustomModelData());
  }

  @Test
  void customModelDataApplied() {
    ItemStack item = new MenuIcon(Material.PAPER, 12345, null, false).build(null, null, false);
    ItemMeta meta = item.getItemMeta();
    assertTrue(meta.hasCustomModelData());
    assertEquals(12345, meta.getCustomModelData());
  }

  @Test
  void glowFromConfigOrOverride() {
    assertTrue(new MenuIcon(Material.PAPER, null, null, true).build(null, null, false)
        .getItemMeta().hasEnchants()
        || new MenuIcon(Material.PAPER, null, null, true).build(null, null, false)
            .getItemMeta().getEnchantmentGlintOverride() != null);
  }

  @Test
  void parsePlainStringEntry() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("money", "GOLD_INGOT");
    MenuIcon icon = MenuIcon.parse(config, "money", Material.PAPER);
    assertEquals(Material.GOLD_INGOT, icon.material());
    assertEquals(null, icon.customModelData());
  }

  @Test
  void parseSectionEntry() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("money.material", "PLAYER_HEAD");
    config.set("money.custom-model-data", 42);
    config.set("money.glow", true);
    MenuIcon icon = MenuIcon.parse(config, "money", Material.PAPER);
    assertEquals(Material.PLAYER_HEAD, icon.material());
    assertEquals(42, icon.customModelData());
    assertTrue(icon.glow());
  }

  @Test
  void invalidMaterialFallsBack() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("money", "NOT_A_REAL_MATERIAL");
    MenuIcon icon = MenuIcon.parse(config, "money", Material.BARRIER);
    assertEquals(Material.BARRIER, icon.material());
  }
}
