package com.splatage.curatedshelves.config;

import org.bukkit.configuration.Configuration;

import java.util.Locale;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static PluginConfig load(final Configuration config) {
        final int rows = Math.max(1, Math.min(4, config.getInt("rows", 1)));
        final boolean authorMustDeposit = config.getBoolean("author-must-deposit", false);
        final boolean authorCanRemoveOwnBook = config.getBoolean("author-can-remove-own-book", false);
        final PluginConfig.StorageType storageType = loadStorageType(config.getString("storage.type", "sqlite"));
        final String tablePrefix = sanitizeTablePrefix(config.getString("storage.table-prefix", ""));
        final PluginConfig.SqliteConfig sqlite = loadSqliteConfig(config, storageType == PluginConfig.StorageType.SQLITE);
        final PluginConfig.MysqlConfig mysql = loadMysqlConfig(config, storageType == PluginConfig.StorageType.MYSQL);
        return new PluginConfig(
                rows,
                authorMustDeposit,
                authorCanRemoveOwnBook,
                new PluginConfig.StorageConfig(storageType, tablePrefix, sqlite, mysql)
        );
    }


    private static PluginConfig.SqliteConfig loadSqliteConfig(final Configuration config, final boolean validate) {
        final String rawFile = config.getString("storage.sqlite.file", "library.db");
        final String file = validate ? sanitizeSqliteFile(rawFile) : (rawFile == null || rawFile.isBlank() ? "library.db" : rawFile.trim());
        return new PluginConfig.SqliteConfig(file);
    }

    private static PluginConfig.MysqlConfig loadMysqlConfig(final Configuration config, final boolean validate) {
        final String host = validate
                ? requireNonBlank(config.getString("storage.mysql.host", "localhost"), "storage.mysql.host")
                : defaultString(config.getString("storage.mysql.host", "localhost"), "localhost");
        final int port = Math.max(1, config.getInt("storage.mysql.port", 3306));
        final String database = validate
                ? requireNonBlank(config.getString("storage.mysql.database", "curated_shelves"), "storage.mysql.database")
                : defaultString(config.getString("storage.mysql.database", "curated_shelves"), "curated_shelves");
        final String username = validate
                ? requireNonBlank(config.getString("storage.mysql.username", "root"), "storage.mysql.username")
                : defaultString(config.getString("storage.mysql.username", "root"), "root");
        final String password = config.getString("storage.mysql.password", "");
        final int maximumPoolSize = Math.max(1, config.getInt("storage.mysql.maximum-pool-size", 10));
        final long connectionTimeoutMillis = Math.max(1000L, config.getLong("storage.mysql.connection-timeout-millis", 10000L));
        return new PluginConfig.MysqlConfig(host, port, database, username, password, maximumPoolSize, connectionTimeoutMillis);
    }

    private static PluginConfig.StorageType loadStorageType(final String rawType) {
        final String normalized = rawType == null ? "sqlite" : rawType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SQLITE" -> PluginConfig.StorageType.SQLITE;
            case "MYSQL" -> PluginConfig.StorageType.MYSQL;
            default -> throw new IllegalArgumentException("Unsupported storage.type: " + rawType);
        };
    }

    private static String sanitizeTablePrefix(final String rawPrefix) {
        final String prefix = rawPrefix == null ? "" : rawPrefix.trim();
        if (prefix.matches("[A-Za-z0-9_]*")) {
            return prefix;
        }
        throw new IllegalArgumentException("storage.table-prefix may only contain letters, numbers, and underscores");
    }

    private static String sanitizeSqliteFile(final String rawFile) {
        final String file = requireNonBlank(rawFile, "storage.sqlite.file");
        if (file.contains("..") || file.contains("/") || file.contains("\\")) {
            throw new IllegalArgumentException("storage.sqlite.file must be a simple file name inside the plugin data folder");
        }
        return file;
    }

    private static String defaultString(final String rawValue, final String fallback) {
        if (rawValue == null) {
            return fallback;
        }
        final String trimmed = rawValue.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static String requireNonBlank(final String rawValue, final String path) {
        if (rawValue == null) {
            throw new IllegalArgumentException(path + " must not be blank");
        }
        final String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(path + " must not be blank");
        }
        return trimmed;
    }
}
