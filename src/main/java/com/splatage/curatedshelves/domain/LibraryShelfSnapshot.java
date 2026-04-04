package com.splatage.curatedshelves.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record LibraryShelfSnapshot(
        LibraryShelf shelf,
        Map<Integer, LibraryBook> booksBySlot
) {
    public LibraryShelfSnapshot {
        Objects.requireNonNull(shelf, "shelf");
        Objects.requireNonNull(booksBySlot, "booksBySlot");
        booksBySlot = Collections.unmodifiableMap(new HashMap<>(booksBySlot));
    }
}
