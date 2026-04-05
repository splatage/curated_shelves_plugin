package com.splatage.curatedshelves.domain;

import java.util.Objects;
import java.util.UUID;

public record LibraryShelf(
        UUID shelfId,
        LocationKey location,
        int rows,
        UUID createdByUuid,
        String createdByName,
        long createdAt,
        long updatedAt
) {
    public LibraryShelf {
        Objects.requireNonNull(shelfId, "shelfId");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(createdByUuid, "createdByUuid");
        Objects.requireNonNull(createdByName, "createdByName");
        if (createdByName.isBlank()) {
            throw new IllegalArgumentException("createdByName must not be blank");
        }
        if (rows < 1 || rows > 4) {
            throw new IllegalArgumentException("rows must be between 1 and 4");
        }
    }

    public LibraryShelf withUpdatedAt(final long newUpdatedAt) {
        return new LibraryShelf(
                this.shelfId,
                this.location,
                this.rows,
                this.createdByUuid,
                this.createdByName,
                this.createdAt,
                newUpdatedAt
        );
    }
}
