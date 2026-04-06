package com.splatage.curatedshelves.data;

import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelf;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public final class SQLiteLibraryRepository extends AbstractJdbcLibraryRepository {
    private final Path databasePath;

    public SQLiteLibraryRepository(final Path databasePath, final String tablePrefix) {
        super(tablePrefix);
        this.databasePath = databasePath;
    }

    @Override
    protected Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + this.databasePath);
    }

    @Override
    protected void prepareConnection(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    @Override
    protected void initializeSchema(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                      shelf_id TEXT PRIMARY KEY,
                      world_uuid TEXT NOT NULL,
                      x INTEGER NOT NULL,
                      y INTEGER NOT NULL,
                      z INTEGER NOT NULL,
                      row_count INTEGER NOT NULL,
                      created_by_uuid TEXT NOT NULL,
                      created_by_name TEXT NOT NULL,
                      created_at INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL
                    )
                    """.formatted(shelvesTable()));
            statement.executeUpdate("""
                    CREATE UNIQUE INDEX IF NOT EXISTS %s
                    ON %s(world_uuid, x, y, z)
                    """.formatted(shelvesLocationIndex(), shelvesTable()));
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                      book_id TEXT PRIMARY KEY,
                      shelf_id TEXT NOT NULL,
                      slot_index INTEGER NOT NULL,
                      item_data TEXT NOT NULL,
                      title TEXT,
                      author TEXT,
                      author_uuid TEXT,
                      shelved_by_uuid TEXT NOT NULL,
                      shelved_by_name TEXT NOT NULL,
                      created_at INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL,
                      UNIQUE (shelf_id, slot_index),
                      FOREIGN KEY (shelf_id) REFERENCES %s(shelf_id) ON DELETE CASCADE
                    )
                    """.formatted(booksTable(), shelvesTable()));
        }
    }

    @Override
    protected void upsertShelf(final Connection connection, final LibraryShelf shelf) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                        INSERT INTO %s (shelf_id, world_uuid, x, y, z, row_count, created_by_uuid, created_by_name, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(shelf_id) DO UPDATE SET
                          world_uuid = excluded.world_uuid,
                          x = excluded.x,
                          y = excluded.y,
                          z = excluded.z,
                          row_count = excluded.row_count,
                          created_by_uuid = excluded.created_by_uuid,
                          created_by_name = excluded.created_by_name,
                          updated_at = excluded.updated_at
                        """.formatted(shelvesTable())
        )) {
            bindShelfParameters(statement, shelf);
            statement.executeUpdate();
        }
    }

    @Override
    protected void upsertBook(final Connection connection, final LibraryBook book) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                        INSERT INTO %s (book_id, shelf_id, slot_index, item_data, title, author, author_uuid, shelved_by_uuid, shelved_by_name, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(book_id) DO UPDATE SET
                          shelf_id = excluded.shelf_id,
                          slot_index = excluded.slot_index,
                          item_data = excluded.item_data,
                          title = excluded.title,
                          author = excluded.author,
                          author_uuid = excluded.author_uuid,
                          shelved_by_uuid = excluded.shelved_by_uuid,
                          shelved_by_name = excluded.shelved_by_name,
                          updated_at = excluded.updated_at
                        """.formatted(booksTable())
        )) {
            bindBookParameters(statement, book);
            statement.executeUpdate();
        }
    }
}
