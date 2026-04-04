package com.splatage.curatedshelves.data;

import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.domain.LocationKey;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SQLiteLibraryRepository implements LibraryRepository {
    private final Path databasePath;
    private Connection connection;

    public SQLiteLibraryRepository(final Path databasePath) {
        this.databasePath = databasePath;
    }

    @Override
    public synchronized void initialize() throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databasePath);
        try (Statement statement = this.connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS shelves (
                      shelf_id TEXT PRIMARY KEY,
                      world_uuid TEXT NOT NULL,
                      x INTEGER NOT NULL,
                      y INTEGER NOT NULL,
                      z INTEGER NOT NULL,
                      rows INTEGER NOT NULL,
                      created_at INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS books (
                      book_id TEXT PRIMARY KEY,
                      shelf_id TEXT NOT NULL,
                      slot_index INTEGER NOT NULL,
                      item_data TEXT NOT NULL,
                      title TEXT,
                      author TEXT,
                      shelved_by_uuid TEXT NOT NULL,
                      shelved_by_name TEXT NOT NULL,
                      created_at INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL,
                      UNIQUE (shelf_id, slot_index)
                    )
                    """);
        }
    }

    @Override
    public synchronized List<LibraryShelf> loadShelves() throws SQLException {
        final List<LibraryShelf> shelves = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(
                "SELECT shelf_id, world_uuid, x, y, z, rows, created_at, updated_at FROM shelves"
        )) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    shelves.add(new LibraryShelf(
                            UUID.fromString(resultSet.getString("shelf_id")),
                            new LocationKey(
                                    UUID.fromString(resultSet.getString("world_uuid")),
                                    resultSet.getInt("x"),
                                    resultSet.getInt("y"),
                                    resultSet.getInt("z")
                            ),
                            resultSet.getInt("rows"),
                            resultSet.getLong("created_at"),
                            resultSet.getLong("updated_at")
                    ));
                }
            }
        }
        return shelves;
    }

    @Override
    public synchronized List<LibraryBook> loadBooks() throws SQLException {
        final List<LibraryBook> books = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(
                "SELECT book_id, shelf_id, slot_index, item_data, title, author, shelved_by_uuid, shelved_by_name, created_at, updated_at FROM books"
        )) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    books.add(new LibraryBook(
                            UUID.fromString(resultSet.getString("book_id")),
                            UUID.fromString(resultSet.getString("shelf_id")),
                            resultSet.getInt("slot_index"),
                            resultSet.getString("item_data"),
                            resultSet.getString("title"),
                            resultSet.getString("author"),
                            UUID.fromString(resultSet.getString("shelved_by_uuid")),
                            resultSet.getString("shelved_by_name"),
                            resultSet.getLong("created_at"),
                            resultSet.getLong("updated_at")
                    ));
                }
            }
        }
        return books;
    }

    @Override
    public synchronized void upsertShelf(final LibraryShelf shelf) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement(
                """
                        INSERT INTO shelves (shelf_id, world_uuid, x, y, z, rows, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(shelf_id) DO UPDATE SET
                          world_uuid = excluded.world_uuid,
                          x = excluded.x,
                          y = excluded.y,
                          z = excluded.z,
                          rows = excluded.rows,
                          updated_at = excluded.updated_at
                        """
        )) {
            statement.setString(1, shelf.shelfId().toString());
            statement.setString(2, shelf.location().worldUuid().toString());
            statement.setInt(3, shelf.location().x());
            statement.setInt(4, shelf.location().y());
            statement.setInt(5, shelf.location().z());
            statement.setInt(6, shelf.rows());
            statement.setLong(7, shelf.createdAt());
            statement.setLong(8, shelf.updatedAt());
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized void deleteShelf(final UUID shelfId) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement("DELETE FROM shelves WHERE shelf_id = ?")) {
            statement.setString(1, shelfId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized void upsertBook(final LibraryBook book) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement(
                """
                        INSERT INTO books (book_id, shelf_id, slot_index, item_data, title, author, shelved_by_uuid, shelved_by_name, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(book_id) DO UPDATE SET
                          shelf_id = excluded.shelf_id,
                          slot_index = excluded.slot_index,
                          item_data = excluded.item_data,
                          title = excluded.title,
                          author = excluded.author,
                          shelved_by_uuid = excluded.shelved_by_uuid,
                          shelved_by_name = excluded.shelved_by_name,
                          updated_at = excluded.updated_at
                        """
        )) {
            statement.setString(1, book.bookId().toString());
            statement.setString(2, book.shelfId().toString());
            statement.setInt(3, book.slotIndex());
            statement.setString(4, book.serializedItem());
            statement.setString(5, book.title());
            statement.setString(6, book.author());
            statement.setString(7, book.shelvedByUuid().toString());
            statement.setString(8, book.shelvedByName());
            statement.setLong(9, book.createdAt());
            statement.setLong(10, book.updatedAt());
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized void deleteBook(final UUID bookId) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement("DELETE FROM books WHERE book_id = ?")) {
            statement.setString(1, bookId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized void deleteBooksForShelf(final UUID shelfId) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement("DELETE FROM books WHERE shelf_id = ?")) {
            statement.setString(1, shelfId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        if (this.connection != null) {
            this.connection.close();
        }
    }

    private Connection requireConnection() {
        if (this.connection == null) {
            throw new IllegalStateException("Repository not initialized");
        }
        return this.connection;
    }
}
