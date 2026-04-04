package com.splatage.curatedshelves.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class BookActionMenuHolder implements InventoryHolder {
    private final UUID shelfId;
    private final UUID bookId;
    private Inventory inventory;

    public BookActionMenuHolder(final UUID shelfId, final UUID bookId) {
        this.shelfId = shelfId;
        this.bookId = bookId;
    }

    public UUID shelfId() {
        return this.shelfId;
    }

    public UUID bookId() {
        return this.bookId;
    }

    public void inventory(final Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
