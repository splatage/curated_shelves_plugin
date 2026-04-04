package com.splatage.curatedshelves.data;

import org.bukkit.inventory.ItemStack;

public final class LibraryBook {
    private final int slotIndex;
    private final ItemStack item;
    private final String title;
    private final String author;
    private final String shelvedByUuid;
    private final String shelvedByName;

    public LibraryBook(
            final int slotIndex,
            final ItemStack item,
            final String title,
            final String author,
            final String shelvedByUuid,
            final String shelvedByName
    ) {
        this.slotIndex = slotIndex;
        this.item = item;
        this.title = title;
        this.author = author;
        this.shelvedByUuid = shelvedByUuid;
        this.shelvedByName = shelvedByName;
    }

    public int slotIndex() {
        return this.slotIndex;
    }

    public ItemStack item() {
        return this.item;
    }

    public String title() {
        return this.title;
    }

    public String author() {
        return this.author;
    }

    public String shelvedByUuid() {
        return this.shelvedByUuid;
    }

    public String shelvedByName() {
        return this.shelvedByName;
    }
}
