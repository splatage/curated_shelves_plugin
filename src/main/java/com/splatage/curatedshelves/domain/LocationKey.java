package com.splatage.curatedshelves.domain;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

public record LocationKey(UUID worldUuid, int x, int y, int z) {
    public LocationKey {
        Objects.requireNonNull(worldUuid, "worldUuid");
    }

    public static LocationKey fromLocation(final Location location) {
        Objects.requireNonNull(location, "location");
        final World world = Objects.requireNonNull(location.getWorld(), "location.world");
        return new LocationKey(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
