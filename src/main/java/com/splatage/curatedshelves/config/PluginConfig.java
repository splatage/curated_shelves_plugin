package com.splatage.curatedshelves.config;

public record PluginConfig(
        int rows,
        boolean authorMustDeposit,
        boolean authorCanRemoveOwnBook,
        StorageConfig storage
) {
    public enum StorageType {
        SQLITE,
        MYSQL
    }

    public record StorageConfig(
            StorageType type,
            String tablePrefix,
            SqliteConfig sqlite,
            MysqlConfig mysql
    ) {
    }

    public record SqliteConfig(
            String file
    ) {
    }

    public record MysqlConfig(
            String host,
            int port,
            String database,
            String username,
            String password,
            int maximumPoolSize,
            long connectionTimeoutMillis
    ) {
    }
}
