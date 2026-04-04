package com.splatage.library.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class BookActionMenuHolder implements InventoryHolder {
    private final UUID shelfId;
    private final int slotIndex;
    private Inventory inventory;

    public BookActionMenuHolder(final UUID shelfId, final int slotIndex) {
        this.shelfId = shelfId;
        this.slotIndex = slotIndex;
    }

    public UUID shelfId() {
        return this.shelfId;
    }

    public int slotIndex() {
        return this.slotIndex;
    }

    public void inventory(final Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
