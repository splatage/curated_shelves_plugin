package com.splatage.curatedshelves.service;

import com.splatage.curatedshelves.util.PdcKeys;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ShelfMarkerService {
    private final PdcKeys pdcKeys;

    public ShelfMarkerService(final PdcKeys pdcKeys) {
        this.pdcKeys = Objects.requireNonNull(pdcKeys, "pdcKeys");
    }

    public boolean isEligibleBlock(final Block block) {
        return block != null && block.getType() == Material.CHISELED_BOOKSHELF && block.getState() instanceof TileState;
    }

    public boolean isMarked(final Block block) {
        if (!isEligibleBlock(block)) {
            return false;
        }
        final TileState tileState = (TileState) block.getState();
        return tileState.getPersistentDataContainer().has(this.pdcKeys.libraryShelf(), PersistentDataType.BYTE)
                && tileState.getPersistentDataContainer().has(this.pdcKeys.shelfId(), PersistentDataType.STRING);
    }

    public boolean hasPhysicalContents(final Block block) {
        if (!(block.getState() instanceof ChiseledBookshelf bookshelf)) {
            return false;
        }
        for (final ItemStack itemStack : bookshelf.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }
            return true;
        }
        return false;
    }

    public Optional<UUID> shelfId(final Block block) {
        if (!isEligibleBlock(block)) {
            return Optional.empty();
        }
        final TileState tileState = (TileState) block.getState();
        final String rawShelfId = tileState.getPersistentDataContainer().get(this.pdcKeys.shelfId(), PersistentDataType.STRING);
        if (rawShelfId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(rawShelfId));
        } catch (final IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public void mark(final Block block, final UUID shelfId) {
        final TileState tileState = requireTileState(block);
        tileState.getPersistentDataContainer().set(this.pdcKeys.libraryShelf(), PersistentDataType.BYTE, (byte) 1);
        tileState.getPersistentDataContainer().set(this.pdcKeys.shelfId(), PersistentDataType.STRING, shelfId.toString());
        tileState.update(true, false);
    }

    public void unmark(final Block block) {
        final TileState tileState = requireTileState(block);
        tileState.getPersistentDataContainer().remove(this.pdcKeys.libraryShelf());
        tileState.getPersistentDataContainer().remove(this.pdcKeys.shelfId());
        tileState.update(true, false);
    }

    private TileState requireTileState(final Block block) {
        if (!(block.getState() instanceof TileState tileState)) {
            throw new IllegalArgumentException("Block is not a tile state");
        }
        return tileState;
    }
}
