package com.arkonas.ranks.menu.items;

import org.bukkit.inventory.ItemStack;

/**
 * Resolves a namespaced item id (e.g. {@code oraxen:rank_icon}, {@code hdb:1234}) to an
 * {@link ItemStack}, provided by an integration with a custom-item plugin. Implementations are
 * registered at enable time only when their plugin is present, so the core never imports a
 * third-party API directly.
 */
public interface NamespacedItemProvider {

  /** The namespace this provider handles, lower-case, without the colon (e.g. {@code "oraxen"}). */
  String namespace();

  /**
   * Creates the item for a key within this provider's namespace.
   *
   * @param key the part after the colon
   * @return the item, or null if the key is unknown / the plugin cannot provide it
   */
  ItemStack create(String key);
}
