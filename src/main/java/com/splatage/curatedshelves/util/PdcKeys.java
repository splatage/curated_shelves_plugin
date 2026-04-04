package com.splatage.curatedshelves.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PdcKeys {
    private final NamespacedKey libraryShelf;
    private final NamespacedKey shelfId;
    private final NamespacedKey badge;
    private final NamespacedKey librarianSeal;

    public PdcKeys(final Plugin plugin) {
        this.libraryShelf = new NamespacedKey(plugin, "library_shelf");
        this.shelfId = new NamespacedKey(plugin, "shelf_id");
        this.badge = new NamespacedKey(plugin, "library_badge");
        this.librarianSeal = new NamespacedKey(plugin, "librarian_seal");
    }

    public NamespacedKey libraryShelf() {
        return this.libraryShelf;
    }

    public NamespacedKey shelfId() {
        return this.shelfId;
    }

    public NamespacedKey badge() {
        return this.badge;
    }

    public NamespacedKey librarianSeal() {
        return this.librarianSeal;
    }
}
