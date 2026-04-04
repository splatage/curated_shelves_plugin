package com.splatage.curatedshelves.config;

public record PluginConfig(
        int rows,
        boolean authorMustDeposit,
        boolean authorCanRemoveOwnBook
) {
}
