package com.arkonas.ranks.menu.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arkonas.ranks.menu.MenuIcon;
import com.arkonas.ranks.menu.RequirementIcons;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Namespaced custom-item resolution and its use as a menu icon. */
class NamespacedItemsTest {

  private static final NamespacedItemProvider FAKE = new NamespacedItemProvider() {
    @Override
    public String namespace() {
      return "test";
    }

    @Override
    public ItemStack create(String key) {
      return key.equals("gem") ? new ItemStack(Material.DIAMOND) : null;
    }
  };

  @BeforeEach
  void setup() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    NamespacedItems.setActive(new NamespacedItems());
    MockBukkit.unmock();
  }

  @Test
  void isNamespacedDetection() {
    assertTrue(NamespacedItems.isNamespaced("oraxen:rank_icon"));
    assertFalse(NamespacedItems.isNamespaced("DIAMOND"));
    assertFalse(NamespacedItems.isNamespaced("bad:"));
    assertFalse(NamespacedItems.isNamespaced(":bad"));
    assertFalse(NamespacedItems.isNamespaced(null));
  }

  @Test
  void registryResolvesKnownAndNull() {
    NamespacedItems registry = new NamespacedItems();
    registry.register(FAKE);
    assertEquals(Material.DIAMOND, registry.create("test:gem").getType());
    assertNull(registry.create("test:unknown"));
    assertNull(registry.create("other:gem"));
    assertNull(registry.create("DIAMOND"));
  }

  @Test
  void menuIconUsesRegisteredProvider() {
    NamespacedItems registry = new NamespacedItems();
    registry.register(FAKE);
    NamespacedItems.setActive(registry);

    YamlConfiguration config = new YamlConfiguration();
    config.set("icon", "test:gem");
    MenuIcon icon = MenuIcon.parse(config, "icon", Material.PAPER);

    assertEquals("test:gem", icon.itemId());
    assertEquals(Material.DIAMOND, icon.build(null, null, false).getType());
  }

  @Test
  void menuIconFallsBackWhenUnresolved() {
    // no provider registered -> namespaced id resolves to null -> fallback material
    YamlConfiguration config = new YamlConfiguration();
    config.set("icon", "test:gem");
    MenuIcon icon = MenuIcon.parse(config, "icon", Material.BARRIER);

    assertEquals(Material.BARRIER, icon.build(null, null, false).getType());
  }

  @Test
  void requirementIconKeepsNamespacedEntry() {
    NamespacedItems registry = new NamespacedItems();
    registry.register(FAKE);
    NamespacedItems.setActive(registry);

    YamlConfiguration config = new YamlConfiguration();
    config.set("default", "PAPER");
    config.set("money", "test:gem"); // namespaced custom item
    RequirementIcons icons = new RequirementIcons(config);

    MenuIcon icon = icons.iconFor("money");
    assertEquals("test:gem", icon.itemId(), "namespaced requirement icon must be retained");
    assertEquals(Material.DIAMOND, icon.build(null, null, false).getType());
  }

  @Test
  void requirementIconNamespacedFallsBackSafely() {
    // no provider registered: the entry is still kept, and build() never NPEs on a null material
    YamlConfiguration config = new YamlConfiguration();
    config.set("money", "test:gem");
    RequirementIcons icons = new RequirementIcons(config);

    assertEquals("test:gem", icons.iconFor("money").itemId());
    assertEquals(Material.PAPER, icons.iconFor("money").build(null, null, false).getType());
  }
}
