package com.splatage.curatedshelves.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class LibraryMenuHolder implements InventoryHolder {
    private final String shelfId;
    private Inventory inventory;

    public LibraryMenuHolder(final String shelfId) {
        this.shelfId = shelfId;
    }

    public String shelfId() {
        return this.shelfId;
    }

    public void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
