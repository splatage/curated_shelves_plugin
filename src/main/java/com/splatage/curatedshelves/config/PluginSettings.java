package com.splatage.curatedshelves.config;

import org.bukkit.configuration.file.FileConfiguration;

public record PluginSettings(
        int rows,
        boolean authorMustDeposit,
        boolean authorCanRemoveOwnBook
) {
    public static PluginSettings fromConfig(final FileConfiguration config) {
        final int configuredRows = config.getInt("rows", 1);
        final int rows = Math.max(1, Math.min(4, configuredRows));
        return new PluginSettings(
                rows,
                config.getBoolean("author-must-deposit", false),
                config.getBoolean("author-can-remove-own-book", false)
        );
    }

    public int size() {
        return this.rows * 9;
    }
}
