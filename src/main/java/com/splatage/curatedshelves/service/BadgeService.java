package com.splatage.curatedshelves.service;

import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.util.PdcKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.UUID;

public final class BadgeService {
    private final PdcKeys pdcKeys;

    public BadgeService(final PdcKeys pdcKeys) {
        this.pdcKeys = Objects.requireNonNull(pdcKeys, "pdcKeys");
    }

    public void ensureBadge(final org.bukkit.block.Block block, final LibraryShelf shelf) {
        removeBadge(block, shelf.shelfId());
        final Location badgeLocation = block.getLocation().add(0.5D, 0.85D, 0.5D);
        final World world = Objects.requireNonNull(badgeLocation.getWorld(), "badgeLocation.world");
        world.spawn(badgeLocation, ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(new ItemStack(Material.LECTERN));
            itemDisplay.setBillboard(Display.Billboard.CENTER);
            itemDisplay.getPersistentDataContainer().set(this.pdcKeys.badge(), PersistentDataType.BYTE, (byte) 1);
            itemDisplay.getPersistentDataContainer().set(this.pdcKeys.shelfId(), PersistentDataType.STRING, shelf.shelfId().toString());
        });
    }

    public void removeBadge(final org.bukkit.block.Block block, final UUID shelfId) {
        final Location center = block.getLocation().add(0.5D, 0.5D, 0.5D);
        final World world = Objects.requireNonNull(center.getWorld(), "center.world");
        for (Entity entity : world.getNearbyEntities(center, 1.25D, 1.25D, 1.25D)) {
            if (!(entity instanceof ItemDisplay itemDisplay)) {
                continue;
            }
            final String rawShelfId = itemDisplay.getPersistentDataContainer().get(this.pdcKeys.shelfId(), PersistentDataType.STRING);
            if (rawShelfId == null) {
                continue;
            }
            if (!shelfId.toString().equals(rawShelfId)) {
                continue;
            }
            itemDisplay.remove();
        }
    }
}
