package com.splatage.curatedshelves.service;

import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.util.PdcKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.UUID;

public final class BadgeService {
    private static final float BADGE_SCALE = 0.5F;

    private final PdcKeys pdcKeys;

    public BadgeService(final PdcKeys pdcKeys) {
        this.pdcKeys = Objects.requireNonNull(pdcKeys, "pdcKeys");
    }

    public void ensureBadge(final org.bukkit.block.Block block, final LibraryShelf shelf) {
        removeBadge(block, shelf.shelfId());
        final World world = Objects.requireNonNull(block.getWorld(), "block.world");
        final BlockFace face = frontFace(block);
        final Location badgeLocation = badgeLocation(block.getLocation(), face);
        world.spawn(badgeLocation, ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(new ItemStack(Material.LECTERN));
            itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            itemDisplay.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(BADGE_SCALE, BADGE_SCALE, BADGE_SCALE),
                    new AxisAngle4f()
            ));
            itemDisplay.setRotation(yawFor(face), 0.0F);
            itemDisplay.getPersistentDataContainer().set(this.pdcKeys.badge(), PersistentDataType.BYTE, (byte) 1);
            itemDisplay.getPersistentDataContainer().set(this.pdcKeys.shelfId(), PersistentDataType.STRING, shelf.shelfId().toString());
        });
    }

    public void removeBadge(final org.bukkit.block.Block block, final UUID shelfId) {
        final Location center = block.getLocation().add(0.5D, 0.5D, 0.5D);
        final World world = Objects.requireNonNull(center.getWorld(), "center.world");
        for (final Entity entity : world.getNearbyEntities(center, 1.5D, 1.5D, 1.5D)) {
            if (!(entity instanceof ItemDisplay) && !(entity instanceof ItemFrame)) {
                continue;
            }
            final String rawShelfId = entity.getPersistentDataContainer().get(this.pdcKeys.shelfId(), PersistentDataType.STRING);
            if (rawShelfId == null) {
                continue;
            }
            if (!shelfId.toString().equals(rawShelfId)) {
                continue;
            }
            entity.remove();
        }
    }

    private BlockFace frontFace(final org.bukkit.block.Block block) {
        if (block.getBlockData() instanceof Directional directional) {
            return directional.getFacing();
        }
        return BlockFace.NORTH;
    }

    private Location badgeLocation(final Location blockLocation, final BlockFace face) {
        return switch (face) {
            case NORTH -> blockLocation.clone().add(0.5D, 0.5D, -0.01D);
            case SOUTH -> blockLocation.clone().add(0.5D, 0.5D, 1.01D);
            case EAST -> blockLocation.clone().add(1.01D, 0.5D, 0.5D);
            case WEST -> blockLocation.clone().add(-0.01D, 0.5D, 0.5D);
            default -> blockLocation.clone().add(0.5D, 0.5D, 0.5D);
        };
    }

    private float yawFor(final BlockFace face) {
        return switch (face) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case EAST -> -90.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}
