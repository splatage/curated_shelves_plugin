package com.splatage.library.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static LibraryConfig load(final FileConfiguration config) {
        final int configuredRows = config.getInt("rows", 1);
        final int rows = Math.max(LibraryConfig.MIN_ROWS, Math.min(LibraryConfig.MAX_ROWS, configuredRows));
        return new LibraryConfig(
                rows,
                config.getBoolean("author-must-deposit", false),
                config.getBoolean("author-can-remove-own-book", false)
        );
    }
}
