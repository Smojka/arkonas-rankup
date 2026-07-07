package com.arkonas.ranks.hook;

public interface PermissionManager {

    GroupProvider findPermissionProvider();

    GroupProvider permissionOnlyProvider();
}
