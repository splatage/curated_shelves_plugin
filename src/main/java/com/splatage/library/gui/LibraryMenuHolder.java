package com.splatage.library.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
