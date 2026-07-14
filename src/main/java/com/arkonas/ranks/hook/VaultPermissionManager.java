package com.arkonas.ranks.hook;

import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.arkonas.ranks.ArkonasRanksPlugin;

public class VaultPermissionManager implements PermissionManager {
  private final ArkonasRanksPlugin plugin;

  public VaultPermissionManager(ArkonasRanksPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public GroupProvider findPermissionProvider() {
    return getVaultPermissionProvider();
  }

  private GroupProvider getVaultPermissionProvider() {
    RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager()
        .getRegistration(Permission.class);
    if (rsp == null) {
      return null;
    }
    Permission provider = rsp.getProvider();
    if (!provider.hasGroupSupport()) {
      return null;
    }
    String lpContext = plugin.getConfig().getString("luckperms-context");
    boolean useLuckPermsGroupNames = plugin.getConfig().getBoolean("use-luckperms-group-names", false);
    if (useLuckPermsGroupNames || (lpContext != null && !lpContext.isEmpty())) {
      // Guard the LuckPerms.class reference behind a plugin-presence check: LuckPerms is compileOnly
      // (not shaded), so touching the class literal when LuckPerms is absent throws
      // NoClassDefFoundError, which previously aborted plugin enable. Fall back to Vault group names.
      if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
        try {
          RegisteredServiceProvider<LuckPerms> lpProvider =
              Bukkit.getServicesManager().getRegistration(LuckPerms.class);
          if (lpProvider != null) {
            return LuckPermsGroupProvider.createFromString(lpProvider.getProvider(), lpContext);
          }
        } catch (Throwable t) {
          plugin.getLogger().warning(
              "Failed to hook LuckPerms for group names; using Vault group names instead: " + t);
        }
      } else {
        plugin.getLogger().warning("use-luckperms-group-names / luckperms-context is set but"
            + " LuckPerms is not installed; using Vault group names instead.");
      }
    }

    return new VaultGroupProvider(provider);
  }

  @Override
  public GroupProvider permissionOnlyProvider() {
    return new PermissionGroupProvider();
  }
}
