package com.splatage.curatedshelves.listener;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionPolicyTest {
    @Test
    void browseOnlyCanOpenFromBrowserButCannotEditOrOpenDirectly() {
        final Player browseOnly = player(Set.of("curatedshelves.admin.browse"));
        assertTrue(InventoryListener.canOpenLibraryMenu(browseOnly));
        assertFalse(InventoryListener.canEditLibraryMenu(browseOnly));
        assertFalse(ShelfInteractListener.canOpenDirectLibraryMenu(browseOnly));
    }

    @Test
    void adminEditHasFullShelfInteractionAuthority() {
        final Player adminEdit = player(Set.of("curatedshelves.admin.edit"));
        assertTrue(InventoryListener.canOpenLibraryMenu(adminEdit));
        assertTrue(InventoryListener.canEditLibraryMenu(adminEdit));
        assertTrue(ShelfInteractListener.canOpenDirectLibraryMenu(adminEdit));
    }

    @Test
    void normalUseCanOpenAndEditShelvesButNotBrowseWithoutOpenPath() {
        final Player user = player(Set.of("curatedshelves.use"));
        assertTrue(InventoryListener.canOpenLibraryMenu(user));
        assertTrue(InventoryListener.canEditLibraryMenu(user));
        assertTrue(ShelfInteractListener.canOpenDirectLibraryMenu(user));
    }

    private Player player(final Set<String> permissions) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("hasPermission") && args != null && args.length == 1) {
                        return permissions.contains((String) args[0]);
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(final Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0F;
        }
        if (returnType == double.class) {
            return 0.0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
