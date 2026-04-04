package com.splatage.library.domain;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record LibraryBookRecord(
        UUID bookId,
        UUID shelfId,
        int slotIndex,
        ItemStack item,
        String title,
        String author,
        UUID shelvedByUuid,
        String shelvedByName,
        long createdAt,
        long updatedAt
) {
    public LibraryBookRecord withSlot(final int slotIndex, final long updatedAt) {
        return new LibraryBookRecord(
                this.bookId,
                this.shelfId,
                slotIndex,
                this.item.clone(),
                this.title,
                this.author,
                this.shelvedByUuid,
                this.shelvedByName,
                this.createdAt,
                updatedAt
        );
    }
}
