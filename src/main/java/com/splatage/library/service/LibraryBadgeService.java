package com.splatage.library.service;

import com.splatage.library.util.PdcKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class LibraryBadgeService {
    private final Plugin plugin;
    private final PdcKeys keys;

    public LibraryBadgeService(final Plugin plugin, final PdcKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    public void ensureBadge(final @NotNull Block block, final @NotNull UUID shelfId) {
        if (this.findBadge(block, shelfId) != null) {
            return;
        }
        final World world = block.getWorld();
        final Location badgeLocation = this.badgeLocation(block);
        world.spawn(badgeLocation, ItemDisplay.class, badge -> {
            badge.setPersistent(true);
            badge.setInvulnerable(true);
            badge.setGravity(false);
            badge.setBillboard(Display.Billboard.FIXED);
            badge.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            badge.setItemStack(ItemStack.of(Material.LECTERN));
            badge.getPersistentDataContainer().set(this.keys.badge, PersistentDataType.BYTE, (byte) 1);
            badge.getPersistentDataContainer().set(this.keys.shelfId, PersistentDataType.STRING, shelfId.toString());
        });
    }

    public void removeBadge(final @NotNull Block block, final @NotNull UUID shelfId) {
        final Entity badge = this.findBadge(block, shelfId);
        if (badge != null) {
            badge.remove();
        }
    }

    private Entity findBadge(final Block block, final UUID shelfId) {
        final Location center = this.badgeLocation(block);
        for (final Entity entity : block.getWorld().getNearbyEntities(center, 0.75, 0.75, 0.75)) {
            if (!(entity instanceof ItemDisplay)) {
                continue;
            }
            final String entityShelfId = entity.getPersistentDataContainer().get(this.keys.shelfId, PersistentDataType.STRING);
            if (entity.getPersistentDataContainer().has(this.keys.badge, PersistentDataType.BYTE)
                    && shelfId.toString().equals(entityShelfId)) {
                return entity;
            }
        }
        return null;
    }

    private Location badgeLocation(final Block block) {
        final Location location = block.getLocation().add(0.5, 0.5, 0.5);
        BlockFace facing = BlockFace.NORTH;
        if (block.getBlockData() instanceof Directional directional) {
            facing = directional.getFacing();
        }
        final org.bukkit.util.Vector direction = facing.getDirection().multiply(0.38);
        location.add(direction);
        location.setYaw(this.yawFor(facing));
        return location;
    }

    private float yawFor(final BlockFace face) {
        return switch (face) {
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }
}
