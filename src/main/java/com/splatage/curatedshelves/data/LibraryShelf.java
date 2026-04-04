package com.splatage.curatedshelves.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LibraryShelf {
    private final String shelfId;
    private final ShelfKey key;
    private final Map<Integer, LibraryBook> booksBySlot;

    public LibraryShelf(final String shelfId, final ShelfKey key) {
        this.shelfId = shelfId;
        this.key = key;
        this.booksBySlot = new HashMap<>();
    }

    public String shelfId() {
        return this.shelfId;
    }

    public ShelfKey key() {
        return this.key;
    }

    public Optional<LibraryBook> getBook(final int slotIndex) {
        return Optional.ofNullable(this.booksBySlot.get(slotIndex));
    }

    public void putBook(final LibraryBook book) {
        this.booksBySlot.put(book.slotIndex(), book);
    }

    public Optional<LibraryBook> removeBook(final int slotIndex) {
        return Optional.ofNullable(this.booksBySlot.remove(slotIndex));
    }

    public Collection<LibraryBook> books() {
        return this.booksBySlot.values();
    }

    public boolean isEmpty() {
        return this.booksBySlot.isEmpty();
    }

    public List<LibraryBook> removeAllBooks() {
        final List<LibraryBook> removed = new ArrayList<>(this.booksBySlot.values());
        this.booksBySlot.clear();
        return removed;
    }

    public int highestUsedSlot() {
        int highest = -1;
        for (final Integer slot : this.booksBySlot.keySet()) {
            if (slot > highest) {
                highest = slot;
            }
        }
        return highest;
    }
}
