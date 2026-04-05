package com.splatage.curatedshelves.data;

import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.domain.LocationKey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractJdbcLibraryRepository implements LibraryRepository {
    private final String tablePrefix;
    private final String shelvesTable;
    private final String booksTable;
    private final String shelvesLocationIndex;

    protected AbstractJdbcLibraryRepository(final String tablePrefix) {
        this.tablePrefix = sanitizeTablePrefix(tablePrefix);
        this.shelvesTable = this.tablePrefix + "shelves";
        this.booksTable = this.tablePrefix + "books";
        this.shelvesLocationIndex = this.tablePrefix + "shelves_world_xyz";
    }

    protected final String tablePrefix() {
        return this.tablePrefix;
    }

    protected final String shelvesTable() {
        return this.shelvesTable;
    }

    protected final String booksTable() {
        return this.booksTable;
    }

    protected final String shelvesLocationIndex() {
        return this.shelvesLocationIndex;
    }

    @Override
    public final void initialize() throws SQLException {
        try (Connection connection = openConnection()) {
            prepareConnection(connection);
            initializeSchema(connection);
        }
    }

    @Override
    public final List<LibraryShelf> loadShelves() throws SQLException {
        final List<LibraryShelf> shelves = new ArrayList<>();
        try (Connection connection = openConnection()) {
            prepareConnection(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT shelf_id, world_uuid, x, y, z, rows, created_by_uuid, created_by_name, created_at, updated_at FROM " + shelvesTable()
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
                                UUID.fromString(resultSet.getString("created_by_uuid")),
                                resultSet.getString("created_by_name"),
                                resultSet.getLong("created_at"),
                                resultSet.getLong("updated_at")
                        ));
                    }
                }
            }
        }
        return shelves;
    }

    @Override
    public final List<LibraryBook> loadBooks() throws SQLException {
        final List<LibraryBook> books = new ArrayList<>();
        try (Connection connection = openConnection()) {
            prepareConnection(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT book_id, shelf_id, slot_index, item_data, title, author, author_uuid, shelved_by_uuid, shelved_by_name, created_at, updated_at FROM " + booksTable()
            )) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        final String rawAuthorUuid = resultSet.getString("author_uuid");
                        books.add(new LibraryBook(
                                UUID.fromString(resultSet.getString("book_id")),
                                UUID.fromString(resultSet.getString("shelf_id")),
                                resultSet.getInt("slot_index"),
                                resultSet.getString("item_data"),
                                resultSet.getString("title"),
                                resultSet.getString("author"),
                                rawAuthorUuid == null || rawAuthorUuid.isBlank() ? null : UUID.fromString(rawAuthorUuid),
                                UUID.fromString(resultSet.getString("shelved_by_uuid")),
                                resultSet.getString("shelved_by_name"),
                                resultSet.getLong("created_at"),
                                resultSet.getLong("updated_at")
                        ));
                    }
                }
            }
        }
        return books;
    }

    @Override
    public final List<UUID> replaceShelfAtLocation(final LibraryShelf shelf) throws SQLException {
        try (Connection connection = openConnection()) {
            prepareConnection(connection);
            final boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                final List<UUID> replacedShelfIds = selectReplacedShelfIds(connection, shelf);
                for (UUID replacedShelfId : replacedShelfIds) {
                    deleteShelfCascade(connection, replacedShelfId);
                }
                upsertShelf(connection, shelf);
                connection.commit();
                return replacedShelfIds;
            } catch (final SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    @Override
    public final void deleteShelfCascade(final UUID shelfId) throws SQLException {
        try (Connection connection = openConnection()) {
            prepareConnection(connection);
            final boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                deleteShelfCascade(connection, shelfId);
                connection.commit();
            } catch (final SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    @Override
    public final void upsertBook(final LibraryBook book) throws SQLException {
        try (Connection connection = openConnection()) {
            prepareConnection(connection);
            upsertBook(connection, book);
        }
    }

    @Override
    public final void deleteBook(final UUID bookId) throws SQLException {
        try (Connection connection = openConnection()) {
            prepareConnection(connection);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + booksTable() + " WHERE book_id = ?")) {
                statement.setString(1, bookId.toString());
                statement.executeUpdate();
            }
        }
    }

    @Override
    public final void deleteOrphanBooks() throws SQLException {
        try (Connection connection = openConnection()) {
            prepareConnection(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + booksTable() + " WHERE shelf_id NOT IN (SELECT shelf_id FROM " + shelvesTable() + ")"
            )) {
                statement.executeUpdate();
            }
        }
    }

    @Override
    public void close() throws SQLException {
    }

    protected abstract Connection openConnection() throws SQLException;

    protected void prepareConnection(final Connection connection) throws SQLException {
    }

    protected abstract void initializeSchema(Connection connection) throws SQLException;

    protected abstract void upsertShelf(Connection connection, LibraryShelf shelf) throws SQLException;

    protected abstract void upsertBook(Connection connection, LibraryBook book) throws SQLException;

    protected final List<UUID> selectReplacedShelfIds(final Connection connection, final LibraryShelf shelf) throws SQLException {
        final List<UUID> replacedShelfIds = new ArrayList<>();
        try (PreparedStatement selectExisting = connection.prepareStatement(
                "SELECT shelf_id FROM " + shelvesTable() + " WHERE world_uuid = ? AND x = ? AND y = ? AND z = ? AND shelf_id <> ?"
        )) {
            selectExisting.setString(1, shelf.location().worldUuid().toString());
            selectExisting.setInt(2, shelf.location().x());
            selectExisting.setInt(3, shelf.location().y());
            selectExisting.setInt(4, shelf.location().z());
            selectExisting.setString(5, shelf.shelfId().toString());
            try (ResultSet resultSet = selectExisting.executeQuery()) {
                while (resultSet.next()) {
                    replacedShelfIds.add(UUID.fromString(resultSet.getString("shelf_id")));
                }
            }
        }
        return replacedShelfIds;
    }

    protected final void deleteShelfCascade(final Connection connection, final UUID shelfId) throws SQLException {
        try (PreparedStatement deleteBooks = connection.prepareStatement("DELETE FROM " + booksTable() + " WHERE shelf_id = ?");
             PreparedStatement deleteShelf = connection.prepareStatement("DELETE FROM " + shelvesTable() + " WHERE shelf_id = ?")) {
            deleteBooks.setString(1, shelfId.toString());
            deleteBooks.executeUpdate();
            deleteShelf.setString(1, shelfId.toString());
            deleteShelf.executeUpdate();
        }
    }

    protected final void bindBookParameters(final PreparedStatement statement, final LibraryBook book) throws SQLException {
        statement.setString(1, book.bookId().toString());
        statement.setString(2, book.shelfId().toString());
        statement.setInt(3, book.slotIndex());
        statement.setString(4, book.serializedItem());
        statement.setString(5, book.title());
        statement.setString(6, book.author());
        if (book.authorUuid() == null) {
            statement.setNull(7, java.sql.Types.VARCHAR);
        } else {
            statement.setString(7, book.authorUuid().toString());
        }
        statement.setString(8, book.shelvedByUuid().toString());
        statement.setString(9, book.shelvedByName());
        statement.setLong(10, book.createdAt());
        statement.setLong(11, book.updatedAt());
    }

    protected final void bindShelfParameters(final PreparedStatement statement, final LibraryShelf shelf) throws SQLException {
        statement.setString(1, shelf.shelfId().toString());
        statement.setString(2, shelf.location().worldUuid().toString());
        statement.setInt(3, shelf.location().x());
        statement.setInt(4, shelf.location().y());
        statement.setInt(5, shelf.location().z());
        statement.setInt(6, shelf.rows());
        statement.setString(7, shelf.createdByUuid().toString());
        statement.setString(8, shelf.createdByName());
        statement.setLong(9, shelf.createdAt());
        statement.setLong(10, shelf.updatedAt());
    }

    private static String sanitizeTablePrefix(final String rawPrefix) {
        final String prefix = rawPrefix == null ? "" : rawPrefix.trim();
        if (prefix.matches("[A-Za-z0-9_]*")) {
            return prefix;
        }
        throw new IllegalArgumentException("storage.table-prefix may only contain letters, numbers, and underscores");
    }
}
