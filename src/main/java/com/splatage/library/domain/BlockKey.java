package com.splatage.library.domain;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record BlockKey(UUID worldId, int x, int y, int z) {
    public static BlockKey fromBlock(final @NotNull Block block) {
        return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockKey fromLocation(final @NotNull Location location) {
        final World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }
        return new BlockKey(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
