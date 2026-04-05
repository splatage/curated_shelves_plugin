package com.splatage.curatedshelves.domain;

import java.util.Objects;
import java.util.UUID;

public record LibraryBook(
        UUID bookId,
        UUID shelfId,
        int slotIndex,
        String serializedItem,
        String title,
        String author,
        UUID authorUuid,
        UUID shelvedByUuid,
        String shelvedByName,
        long createdAt,
        long updatedAt
) {
    public LibraryBook {
        Objects.requireNonNull(bookId, "bookId");
        Objects.requireNonNull(shelfId, "shelfId");
        Objects.requireNonNull(serializedItem, "serializedItem");
        Objects.requireNonNull(shelvedByUuid, "shelvedByUuid");
        Objects.requireNonNull(shelvedByName, "shelvedByName");
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex must be >= 0");
        }
    }

    public LibraryBook withSlotIndex(final int newSlotIndex, final long newUpdatedAt) {
        return new LibraryBook(
                this.bookId,
                this.shelfId,
                newSlotIndex,
                this.serializedItem,
                this.title,
                this.author,
                this.authorUuid,
                this.shelvedByUuid,
                this.shelvedByName,
                this.createdAt,
                newUpdatedAt
        );
    }
}
