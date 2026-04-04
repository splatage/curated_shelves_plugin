package com.splatage.curatedshelves.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class LibraryMenuHolder implements InventoryHolder {
    private final UUID shelfId;
    private Inventory inventory;

    public LibraryMenuHolder(final UUID shelfId) {
        this.shelfId = shelfId;
    }

    public UUID shelfId() {
        return this.shelfId;
    }

    public void inventory(final Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
