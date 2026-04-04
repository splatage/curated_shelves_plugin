package com.splatage.curatedshelves.service;

import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.util.PdcKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Rotation;

import java.util.Objects;
import java.util.UUID;

public final class BadgeService {
    private static final BlockFace[] BADGE_FACES = {
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    };

    private final PdcKeys pdcKeys;

    public BadgeService(final PdcKeys pdcKeys) {
        this.pdcKeys = Objects.requireNonNull(pdcKeys, "pdcKeys");
    }

    public void ensureBadge(final org.bukkit.block.Block block, final LibraryShelf shelf) {
        removeBadge(block, shelf.shelfId());
        final World world = Objects.requireNonNull(block.getWorld(), "block.world");
        for (BlockFace face : BADGE_FACES) {
            final Location badgeLocation = block.getRelative(face).getLocation().add(0.5D, 0.5D, 0.5D);
            world.spawn(badgeLocation, ItemFrame.class, itemFrame -> {
                itemFrame.setItem(new ItemStack(Material.LECTERN), false);
                itemFrame.setFacingDirection(face, true);
                itemFrame.setVisible(false);
                itemFrame.setFixed(true);
                itemFrame.setRotation(Rotation.NONE);
                itemFrame.getPersistentDataContainer().set(this.pdcKeys.badge(), PersistentDataType.BYTE, (byte) 1);
                itemFrame.getPersistentDataContainer().set(this.pdcKeys.shelfId(), PersistentDataType.STRING, shelf.shelfId().toString());
            });
        }
    }

    public void removeBadge(final org.bukkit.block.Block block, final UUID shelfId) {
        final Location center = block.getLocation().add(0.5D, 0.5D, 0.5D);
        final World world = Objects.requireNonNull(center.getWorld(), "center.world");
        for (Entity entity : world.getNearbyEntities(center, 1.5D, 1.5D, 1.5D)) {
            if (!(entity instanceof ItemFrame) && !(entity instanceof ItemDisplay)) {
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
}
