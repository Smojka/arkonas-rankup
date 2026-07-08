package com.arkonas.ranks.menu.items;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

/**
 * Registry of {@link NamespacedItemProvider}s, letting menu icons reference custom items from
 * plugins like Oraxen, ItemsAdder, Nexo or HeadDatabase by a {@code namespace:key} id. Providers
 * are registered at enable time (only when their plugin is present); the shared {@link #active()}
 * instance is what {@link com.arkonas.ranks.menu.MenuIcon} consults when building a namespaced
 * icon. With no providers registered, namespaced ids resolve to null and the icon falls back to a
 * plain material.
 */
public final class NamespacedItems {

  private static NamespacedItems active = new NamespacedItems();

  private final Map<String, NamespacedItemProvider> providers = new HashMap<>();

  public static NamespacedItems active() {
    return active;
  }

  public static void setActive(NamespacedItems registry) {
    active = registry == null ? new NamespacedItems() : registry;
  }

  public void register(NamespacedItemProvider provider) {
    providers.put(provider.namespace().toLowerCase(Locale.ROOT), provider);
  }

  /** True when a string looks like a namespaced id ({@code ns:key}) rather than a material name. */
  public static boolean isNamespaced(String value) {
    if (value == null) {
      return false;
    }
    int colon = value.indexOf(':');
    return colon > 0 && colon < value.length() - 1;
  }

  /**
   * Resolves a namespaced id to an item, or null if the namespace is unregistered or the provider
   * cannot supply the key.
   */
  public ItemStack create(String id) {
    if (!isNamespaced(id)) {
      return null;
    }
    int colon = id.indexOf(':');
    String namespace = id.substring(0, colon).toLowerCase(Locale.ROOT);
    String key = id.substring(colon + 1);
    NamespacedItemProvider provider = providers.get(namespace);
    return provider == null ? null : provider.create(key);
  }
}
