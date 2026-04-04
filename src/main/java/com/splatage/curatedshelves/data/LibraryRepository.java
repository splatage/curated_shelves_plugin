package com.splatage.curatedshelves.data;

import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelf;

import java.sql.SQLException;
import java.util.List;

public interface LibraryRepository {
    void initialize() throws SQLException;

    List<LibraryShelf> loadShelves() throws SQLException;

    List<LibraryBook> loadBooks() throws SQLException;

    void upsertShelf(LibraryShelf shelf) throws SQLException;

    void deleteShelf(java.util.UUID shelfId) throws SQLException;

    void upsertBook(LibraryBook book) throws SQLException;

    void deleteBook(java.util.UUID bookId) throws SQLException;

    void deleteBooksForShelf(java.util.UUID shelfId) throws SQLException;

    void close() throws SQLException;
}
