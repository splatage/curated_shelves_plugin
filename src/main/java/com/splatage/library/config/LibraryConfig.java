package com.splatage.library.config;

public record LibraryConfig(
        int rows,
        boolean authorMustDeposit,
        boolean authorCanRemoveOwnBook
) {
    public static final int MIN_ROWS = 1;
    public static final int MAX_ROWS = 4;

    public int slotCount() {
        return this.rows * 9;
    }
}
