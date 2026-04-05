package com.splatage.curatedshelves.data;

import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelf;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface LibraryRepository {
    void initialize() throws SQLException;

    List<LibraryShelf> loadShelves() throws SQLException;

    List<LibraryBook> loadBooks() throws SQLException;

    List<UUID> replaceShelfAtLocation(LibraryShelf shelf) throws SQLException;

    void deleteShelfCascade(UUID shelfId) throws SQLException;

    void upsertBook(LibraryBook book) throws SQLException;

    void deleteBook(UUID bookId) throws SQLException;

    void deleteOrphanBooks() throws SQLException;

    void close() throws SQLException;
}
