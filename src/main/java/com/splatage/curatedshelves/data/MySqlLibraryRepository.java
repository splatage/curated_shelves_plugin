package com.splatage.curatedshelves.data;

import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelf;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class MySqlLibraryRepository extends AbstractJdbcLibraryRepository {
    private final HikariDataSource dataSource;

    public MySqlLibraryRepository(
            final String host,
            final int port,
            final String database,
            final String username,
            final String password,
            final int maximumPoolSize,
            final long connectionTimeoutMillis,
            final String tablePrefix
    ) {
        super(tablePrefix);
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("curated-shelves-mysql");
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(maximumPoolSize);
        hikariConfig.setMinimumIdle(Math.min(2, maximumPoolSize));
        hikariConfig.setConnectionTimeout(connectionTimeoutMillis);
        hikariConfig.setValidationTimeout(Math.min(connectionTimeoutMillis, 5000L));
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    @Override
    protected Connection openConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    @Override
    protected void initializeSchema(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                      shelf_id VARCHAR(36) PRIMARY KEY,
                      world_uuid VARCHAR(36) NOT NULL,
                      x INT NOT NULL,
                      y INT NOT NULL,
                      z INT NOT NULL,
                      rows INT NOT NULL,
                      created_by_uuid VARCHAR(36) NOT NULL,
                      created_by_name VARCHAR(255) NOT NULL,
                      created_at BIGINT NOT NULL,
                      updated_at BIGINT NOT NULL
                    ) ENGINE=InnoDB
                    """.formatted(shelvesTable()));
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                      book_id VARCHAR(36) PRIMARY KEY,
                      shelf_id VARCHAR(36) NOT NULL,
                      slot_index INT NOT NULL,
                      item_data LONGTEXT NOT NULL,
                      title TEXT,
                      author TEXT,
                      author_uuid VARCHAR(36),
                      shelved_by_uuid VARCHAR(36) NOT NULL,
                      shelved_by_name VARCHAR(255) NOT NULL,
                      created_at BIGINT NOT NULL,
                      updated_at BIGINT NOT NULL,
                      UNIQUE KEY %s_slot_unique (shelf_id, slot_index),
                      CONSTRAINT %s_shelf_fk FOREIGN KEY (shelf_id) REFERENCES %s(shelf_id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """.formatted(booksTable(), booksTable(), booksTable(), shelvesTable()));
        }
        ensureUniqueLocationIndex(connection);
        ensureColumnExists(connection, booksTable(), "author_uuid", "VARCHAR(36)");
    }

    @Override
    protected void upsertShelf(final Connection connection, final LibraryShelf shelf) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                        INSERT INTO %s (shelf_id, world_uuid, x, y, z, rows, created_by_uuid, created_by_name, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                          world_uuid = VALUES(world_uuid),
                          x = VALUES(x),
                          y = VALUES(y),
                          z = VALUES(z),
                          rows = VALUES(rows),
                          created_by_uuid = VALUES(created_by_uuid),
                          created_by_name = VALUES(created_by_name),
                          updated_at = VALUES(updated_at)
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
                        ON DUPLICATE KEY UPDATE
                          shelf_id = VALUES(shelf_id),
                          slot_index = VALUES(slot_index),
                          item_data = VALUES(item_data),
                          title = VALUES(title),
                          author = VALUES(author),
                          author_uuid = VALUES(author_uuid),
                          shelved_by_uuid = VALUES(shelved_by_uuid),
                          shelved_by_name = VALUES(shelved_by_name),
                          updated_at = VALUES(updated_at)
                        """.formatted(booksTable())
        )) {
            bindBookParameters(statement, book);
            statement.executeUpdate();
        }
    }

    @Override
    public void close() {
        this.dataSource.close();
    }

    private void ensureUniqueLocationIndex(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?"
        )) {
            statement.setString(1, shelvesTable());
            statement.setString(2, shelvesLocationIndex());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE UNIQUE INDEX " + shelvesLocationIndex() + " ON " + shelvesTable() + "(world_uuid, x, y, z)");
        }
    }
}
