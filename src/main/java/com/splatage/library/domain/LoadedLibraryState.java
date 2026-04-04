package com.splatage.library.domain;

import java.util.List;

public record LoadedLibraryState(
        List<LibraryShelfRecord> shelves,
        List<LibraryBookRecord> books
) {
}
