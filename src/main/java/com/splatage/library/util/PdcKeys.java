package com.splatage.library.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PdcKeys {
    public final NamespacedKey libraryShelf;
    public final NamespacedKey shelfId;
    public final NamespacedKey librarianSeal;
    public final NamespacedKey badge;

    public PdcKeys(final Plugin plugin) {
        this.libraryShelf = new NamespacedKey(plugin, "library_shelf");
        this.shelfId = new NamespacedKey(plugin, "shelf_id");
        this.librarianSeal = new NamespacedKey(plugin, "librarian_seal");
        this.badge = new NamespacedKey(plugin, "library_badge");
    }
}
