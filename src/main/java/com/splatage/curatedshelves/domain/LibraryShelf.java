package com.splatage.curatedshelves.domain;

import java.util.Objects;
import java.util.UUID;

public record LibraryShelf(
        UUID shelfId,
        LocationKey location,
        int rows,
        long createdAt,
        long updatedAt
) {
    public LibraryShelf {
        Objects.requireNonNull(shelfId, "shelfId");
        Objects.requireNonNull(location, "location");
        if (rows < 1 || rows > 4) {
            throw new IllegalArgumentException("rows must be between 1 and 4");
        }
    }

    public LibraryShelf withUpdatedAt(final long newUpdatedAt) {
        return new LibraryShelf(this.shelfId, this.location, this.rows, this.createdAt, newUpdatedAt);
    }
}
