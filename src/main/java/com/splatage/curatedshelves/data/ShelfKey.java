package com.splatage.curatedshelves.data;

import org.bukkit.block.Block;

import java.util.UUID;

public record ShelfKey(String worldUid, int x, int y, int z) {
    public static ShelfKey fromBlock(final Block block) {
        return new ShelfKey(block.getWorld().getUID().toString(), block.getX(), block.getY(), block.getZ());
    }

    public UUID worldUuid() {
        return UUID.fromString(this.worldUid);
    }
}
