package com.splatage.curatedshelves.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.UUID;

public final class ShelfBrowserMenuHolder implements InventoryHolder {
    private final int page;
    private final int totalPages;
    private final List<UUID> shelfIdsBySlot;
    private Inventory inventory;

    public ShelfBrowserMenuHolder(final int page, final int totalPages, final List<UUID> shelfIdsBySlot) {
        this.page = page;
        this.totalPages = totalPages;
        this.shelfIdsBySlot = java.util.Collections.unmodifiableList(shelfIdsBySlot);
    }

    public int page() {
        return this.page;
    }

    public int totalPages() {
        return this.totalPages;
    }

    public UUID shelfIdAt(final int rawSlot) {
        if (rawSlot < 0 || rawSlot >= this.shelfIdsBySlot.size()) {
            return null;
        }
        return this.shelfIdsBySlot.get(rawSlot);
    }

    public void inventory(final Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
