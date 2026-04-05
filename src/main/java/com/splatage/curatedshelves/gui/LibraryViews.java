package com.splatage.curatedshelves.gui;

import com.splatage.curatedshelves.domain.LibraryShelfSnapshot;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class LibraryViews {
    private LibraryViews() {
    }

    public static void openShelfMenu(final Player player, final LibraryShelfSnapshot snapshot) {
        player.openInventory(LibraryMenu.create(snapshot));
    }

    public static void openBookActionMenu(final Player player, final UUID shelfId, final UUID bookId) {
        player.openInventory(BookActionMenu.create(shelfId, bookId));
    }

    public static void openShelfBrowserMenu(final Player player, final List<LibraryShelfSnapshot> snapshots, final int page) {
        player.openInventory(ShelfBrowserMenu.create(snapshots, page));
    }
}
