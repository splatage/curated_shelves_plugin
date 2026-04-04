package com.splatage.library.persistence;

import com.splatage.library.domain.BlockKey;
import com.splatage.library.domain.LibraryBookRecord;
import com.splatage.library.domain.LibraryShelfRecord;
import com.splatage.library.domain.LoadedLibraryState;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SQLiteLibraryRepository {
    private final String jdbcUrl;

    public SQLiteLibraryRepository(final File dataFolder) {
        try {
            Files.createDirectories(dataFolder.toPath());
            Class.forName("org.sqlite.JDBC");
        } catch (final Exception exception) {
            throw new IllegalStateException("Unable to initialise SQLite repository", exception);
        }
        this.jdbcUrl = "jdbc:sqlite:" + new File(dataFolder, "library.db").getAbsolutePath();
    }

    public void init() {
        try (Connection connection = this.connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS library_shelves (
                      shelf_id TEXT PRIMARY KEY,
                      world_uid TEXT NOT NULL,
                      x INTEGER NOT NULL,
                      y INTEGER NOT NULL,
                      z INTEGER NOT NULL,
                      created_at INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS library_books (
                      book_id TEXT PRIMARY KEY,
                      shelf_id TEXT NOT NULL,
                      slot_index INTEGER NOT NULL,
                      item_bytes BLOB NOT NULL,
                      title TEXT NOT NULL,
                      author TEXT NOT NULL,
                      shelved_by_uuid TEXT NOT NULL,
                      shelved_by_name TEXT NOT NULL,
                      created_at INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL,
                      UNIQUE (shelf_id, slot_index)
                    )
                    """);
        } catch (final SQLException exception) {
            throw new IllegalStateException("Unable to initialise library schema", exception);
        }
    }

    public LoadedLibraryState loadAll() {
        final List<LibraryShelfRecord> shelves = new ArrayList<>();
        final List<LibraryBookRecord> books = new ArrayList<>();
        try (Connection connection = this.connection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT shelf_id, world_uid, x, y, z, created_at, updated_at FROM library_shelves");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    shelves.add(new LibraryShelfRecord(
                            UUID.fromString(resultSet.getString("shelf_id")),
                            new BlockKey(
                                    UUID.fromString(resultSet.getString("world_uid")),
                                    resultSet.getInt("x"),
                                    resultSet.getInt("y"),
                                    resultSet.getInt("z")
                            ),
                            resultSet.getLong("created_at"),
                            resultSet.getLong("updated_at")
                    ));
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT book_id, shelf_id, slot_index, item_bytes, title, author, shelved_by_uuid, shelved_by_name, created_at, updated_at FROM library_books");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    books.add(new LibraryBookRecord(
                            UUID.fromString(resultSet.getString("book_id")),
                            UUID.fromString(resultSet.getString("shelf_id")),
                            resultSet.getInt("slot_index"),
                            ItemStack.deserializeBytes(resultSet.getBytes("item_bytes")),
                            resultSet.getString("title"),
                            resultSet.getString("author"),
                            UUID.fromString(resultSet.getString("shelved_by_uuid")),
                            resultSet.getString("shelved_by_name"),
                            resultSet.getLong("created_at"),
                            resultSet.getLong("updated_at")
                    ));
                }
            }
        } catch (final SQLException exception) {
            throw new IllegalStateException("Unable to load library state", exception);
        }
        return new LoadedLibraryState(shelves, books);
    }

    public void upsertShelf(final LibraryShelfRecord shelfRecord) {
        try (Connection connection = this.connection(); PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO library_shelves (shelf_id, world_uid, x, y, z, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(shelf_id) DO UPDATE SET
                  world_uid = excluded.world_uid,
                  x = excluded.x,
                  y = excluded.y,
                  z = excluded.z,
                  updated_at = excluded.updated_at
                """)) {
            statement.setString(1, shelfRecord.shelfId().toString());
            statement.setString(2, shelfRecord.blockKey().worldId().toString());
            statement.setInt(3, shelfRecord.blockKey().x());
            statement.setInt(4, shelfRecord.blockKey().y());
            statement.setInt(5, shelfRecord.blockKey().z());
            statement.setLong(6, shelfRecord.createdAt());
            statement.setLong(7, shelfRecord.updatedAt());
            statement.executeUpdate();
        } catch (final SQLException exception) {
            throw new IllegalStateException("Unable to save library shelf", exception);
        }
    }

    public void deleteShelf(final UUID shelfId) {
        try (Connection connection = this.connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteBooks = connection.prepareStatement("DELETE FROM library_books WHERE shelf_id = ?");
                 PreparedStatement deleteShelf = connection.prepareStatement("DELETE FROM library_shelves WHERE shelf_id = ?")) {
                deleteBooks.setString(1, shelfId.toString());
                deleteBooks.executeUpdate();
                deleteShelf.setString(1, shelfId.toString());
                deleteShelf.executeUpdate();
                connection.commit();
            } catch (final SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (final SQLException exception) {
            throw new IllegalStateException("Unable to delete library shelf", exception);
        }
    }

    public void upsertBook(final LibraryBookRecord bookRecord) {
        try (Connection connection = this.connection(); PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO library_books (book_id, shelf_id, slot_index, item_bytes, title, author, shelved_by_uuid, shelved_by_name, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(book_id) DO UPDATE SET
                  shelf_id = excluded.shelf_id,
                  slot_index = excluded.slot_index,
                  item_bytes = excluded.item_bytes,
                  title = excluded.title,
                  author = excluded.author,
                  shelved_by_uuid = excluded.shelved_by_uuid,
                  shelved_by_name = excluded.shelved_by_name,
                  updated_at = excluded.updated_at
                """)) {
            statement.setString(1, bookRecord.bookId().toString());
            statement.setString(2, bookRecord.shelfId().toString());
            statement.setInt(3, bookRecord.slotIndex());
            statement.setBytes(4, bookRecord.item().serializeAsBytes());
            statement.setString(5, bookRecord.title());
            statement.setString(6, bookRecord.author());
            statement.setString(7, bookRecord.shelvedByUuid().toString());
            statement.setString(8, bookRecord.shelvedByName());
            statement.setLong(9, bookRecord.createdAt());
            statement.setLong(10, bookRecord.updatedAt());
            statement.executeUpdate();
        } catch (final SQLException exception) {
            throw new IllegalStateException("Unable to save library book", exception);
        }
    }

    public void deleteBook(final UUID bookId) {
        try (Connection connection = this.connection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM library_books WHERE book_id = ?")) {
            statement.setString(1, bookId.toString());
            statement.executeUpdate();
        } catch (final SQLException exception) {
            throw new IllegalStateException("Unable to delete library book", exception);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl);
    }
}
