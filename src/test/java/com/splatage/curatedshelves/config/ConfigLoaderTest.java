package com.splatage.curatedshelves.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLoaderTest {
    @Test
    void sqliteConfigDoesNotRequireMysqlFields() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("storage.type", "sqlite");
        config.set("storage.sqlite.file", "library.db");
        config.set("storage.mysql.host", "");
        config.set("storage.mysql.database", "");
        config.set("storage.mysql.username", "");

        final PluginConfig loaded = assertDoesNotThrow(() -> ConfigLoader.load(config));
        assertEquals(PluginConfig.StorageType.SQLITE, loaded.storage().type());
        assertEquals("library.db", loaded.storage().sqlite().file());
    }

    @Test
    void mysqlConfigRequiresMysqlFields() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("storage.type", "mysql");
        config.set("storage.mysql.host", "");
        config.set("storage.mysql.database", "curated_shelves");
        config.set("storage.mysql.username", "root");

        assertThrows(IllegalArgumentException.class, () -> ConfigLoader.load(config));
    }
}
