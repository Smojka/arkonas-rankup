package com.arkonas.ranks.menu.items;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Builds and installs the concrete {@link NamespacedItemProvider}s for the custom-item plugins we
 * support (Oraxen, Nexo, ItemsAdder, HeadDatabase), so menu icons written as {@code oraxen:key},
 * {@code nexo:key}, {@code itemsadder:pack:key} or {@code hdb:id} resolve to the real item.
 *
 * <p>Everything is resolved by reflection with the method handles bound up front, so the core keeps
 * no compile dependency on any of them and a provider whose API drifts simply resolves to null
 * (the icon then falls back to a plain material). Only plugins that are actually enabled are
 * registered.
 */
public final class NamespacedItemHooks {

  private NamespacedItemHooks() {
  }

  /** Registers a provider for every supported custom-item plugin that is enabled, then publishes the
   *  populated registry as the active one consulted by {@link com.arkonas.ranks.menu.MenuIcon}. */
  public static void install(Logger logger) {
    NamespacedItems registry = new NamespacedItems();
    register(registry, logger, "Oraxen",
        viaBuilder("oraxen", "io.th0rgal.oraxen.api.OraxenItems", "getItemById"));
    register(registry, logger, "Nexo",
        viaBuilder("nexo", "com.nexomc.nexo.api.NexoItems", "itemFromId"));
    register(registry, logger, "ItemsAdder", itemsAdder());
    register(registry, logger, "HeadDatabase", headDatabase());
    NamespacedItems.setActive(registry);
  }

  private static void register(NamespacedItems registry, Logger logger, String plugin,
      NamespacedItemProvider provider) {
    if (!Bukkit.getPluginManager().isPluginEnabled(plugin)) {
      return;
    }
    if (provider == null) {
      logger.warning(plugin + " is installed but its item API could not be bound; menu icons using"
          + " its namespace will fall back to a plain material.");
      return;
    }
    registry.register(provider);
    logger.info("Hooked " + plugin + " for custom menu-icon items.");
  }

  /** Oraxen/Nexo shape: a static {@code get(String) -> ItemBuilder} whose {@code build()} yields the
   *  stack. Returns null (skip) if the classes/methods are not present. */
  private static NamespacedItemProvider viaBuilder(String namespace, String className,
      String getMethod) {
    try {
      Class<?> itemsClass = Class.forName(className);
      Method get = itemsClass.getMethod(getMethod, String.class);
      return reflective(namespace, key -> {
        Object builder = get.invoke(null, key);
        if (builder == null) {
          return null;
        }
        Object stack = builder.getClass().getMethod("build").invoke(builder);
        return stack instanceof ItemStack ? (ItemStack) stack : null;
      });
    } catch (Throwable t) {
      return null;
    }
  }

  /** ItemsAdder: {@code CustomStack.getInstance(fullId).getItemStack()}. The key keeps the item's
   *  own {@code pack:name} form because {@link NamespacedItems} only splits on the first colon. */
  private static NamespacedItemProvider itemsAdder() {
    try {
      Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
      Method getInstance = customStack.getMethod("getInstance", String.class);
      Method getItemStack = customStack.getMethod("getItemStack");
      return reflective("itemsadder", key -> {
        Object stack = getInstance.invoke(null, key);
        if (stack == null) {
          return null;
        }
        Object item = getItemStack.invoke(stack);
        return item instanceof ItemStack ? (ItemStack) item : null;
      });
    } catch (Throwable t) {
      return null;
    }
  }

  /** HeadDatabase: {@code new HeadDatabaseAPI().getItemHead(id)}. */
  private static NamespacedItemProvider headDatabase() {
    try {
      Class<?> apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
      Object api = apiClass.getDeclaredConstructor().newInstance();
      Method getItemHead = apiClass.getMethod("getItemHead", String.class);
      return reflective("hdb", key -> {
        Object head = getItemHead.invoke(api, key);
        return head instanceof ItemStack ? (ItemStack) head : null;
      });
    } catch (Throwable t) {
      return null;
    }
  }

  /** Wraps a reflective resolver in a fail-closed provider (any failure -> null -> material fallback). */
  private static NamespacedItemProvider reflective(String namespace, ReflectiveResolver resolver) {
    return new NamespacedItemProvider() {
      @Override
      public String namespace() {
        return namespace;
      }

      @Override
      public ItemStack create(String key) {
        try {
          return resolver.resolve(key);
        } catch (Throwable t) {
          return null;
        }
      }
    };
  }

  /** A resolver that may throw reflective exceptions; the wrapper turns those into a null result. */
  private interface ReflectiveResolver {
    ItemStack resolve(String key) throws Exception;
  }
}
