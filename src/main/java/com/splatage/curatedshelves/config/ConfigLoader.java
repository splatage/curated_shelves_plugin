package com.splatage.curatedshelves.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static PluginConfig load(final FileConfiguration config) {
        final int rows = Math.max(1, Math.min(4, config.getInt("rows", 1)));
        final boolean authorMustDeposit = config.getBoolean("author-must-deposit", false);
        final boolean authorCanRemoveOwnBook = config.getBoolean("author-can-remove-own-book", false);
        return new PluginConfig(rows, authorMustDeposit, authorCanRemoveOwnBook);
    }
}
