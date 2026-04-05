package com.splatage.curatedshelves.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLoaderTest {
    @Test
    void loadsMysqlStorageConfigAndTablePrefix() {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("rows", 3);
        configuration.set("author-must-deposit", true);
        configuration.set("author-can-remove-own-book", true);
        configuration.set("storage.type", "mysql");
        configuration.set("storage.table-prefix", "wild_");
        configuration.set("storage.mysql.host", "db.example.com");
        configuration.set("storage.mysql.port", 3307);
        configuration.set("storage.mysql.database", "curated_shelves");
        configuration.set("storage.mysql.username", "user");
        configuration.set("storage.mysql.password", "secret");
        configuration.set("storage.mysql.maximum-pool-size", 12);
        configuration.set("storage.mysql.connection-timeout-millis", 15000L);

        final PluginConfig config = ConfigLoader.load(configuration);

        assertEquals(PluginConfig.StorageType.MYSQL, config.storage().type());
        assertEquals("wild_", config.storage().tablePrefix());
        assertEquals("db.example.com", config.storage().mysql().host());
        assertEquals(3307, config.storage().mysql().port());
        assertEquals(12, config.storage().mysql().maximumPoolSize());
        assertEquals(15000L, config.storage().mysql().connectionTimeoutMillis());
    }

    @Test
    void rejectsUnsafeSqliteFileNames() {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("storage.sqlite.file", "../library.db");

        assertThrows(IllegalArgumentException.class, () -> ConfigLoader.load(configuration));
    }

    @Test
    void rejectsInvalidTablePrefixCharacters() {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("storage.table-prefix", "bad-prefix");

        assertThrows(IllegalArgumentException.class, () -> ConfigLoader.load(configuration));
    }
}
