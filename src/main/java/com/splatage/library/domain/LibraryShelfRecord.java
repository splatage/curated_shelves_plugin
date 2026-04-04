package com.splatage.library.domain;

import java.util.UUID;

public record LibraryShelfRecord(
        UUID shelfId,
        BlockKey blockKey,
        long createdAt,
        long updatedAt
) {
    public LibraryShelfRecord touch(final long timestamp) {
        return new LibraryShelfRecord(this.shelfId, this.blockKey, this.createdAt, timestamp);
    }
}
