package com.arkonas.ranks.providers;

import com.arkonas.ranks.hook.GroupProvider;
import com.arkonas.ranks.hook.PermissionManager;

public class TestPermissionManager implements PermissionManager {
    private final GroupProvider groupProvider;

    public TestPermissionManager(GroupProvider groupProvider) {
        this.groupProvider = groupProvider;
    }

    @Override
    public GroupProvider findPermissionProvider() {
        return groupProvider;
    }

    @Override
    public GroupProvider permissionOnlyProvider() {
        return groupProvider;
    }
}
