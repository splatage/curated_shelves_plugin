package com.splatage.curatedshelves.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PdcKeys {
    private final NamespacedKey shelfMarker;
    private final NamespacedKey shelfId;
    private final NamespacedKey badgeMarker;
    private final NamespacedKey sealMarker;

    public PdcKeys(final Plugin plugin) {
        this.shelfMarker = new NamespacedKey(plugin, "library_shelf");
        this.shelfId = new NamespacedKey(plugin, "library_shelf_id");
        this.badgeMarker = new NamespacedKey(plugin, "library_badge");
        this.sealMarker = new NamespacedKey(plugin, "librarians_seal");
    }

    public NamespacedKey shelfMarker() {
        return this.shelfMarker;
    }

    public NamespacedKey shelfId() {
        return this.shelfId;
    }

    public NamespacedKey badgeMarker() {
        return this.badgeMarker;
    }

    public NamespacedKey sealMarker() {
        return this.sealMarker;
    }
}
