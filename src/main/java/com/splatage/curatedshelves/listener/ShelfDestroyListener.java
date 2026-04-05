package com.splatage.curatedshelves.listener;

import com.splatage.curatedshelves.CuratedShelvesPlugin;
import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelfSnapshot;
import com.splatage.curatedshelves.service.BadgeService;
import com.splatage.curatedshelves.service.LibraryService;
import com.splatage.curatedshelves.service.ShelfMarkerService;
import com.splatage.curatedshelves.util.BookCodec;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

public final class ShelfDestroyListener implements Listener {
    private final CuratedShelvesPlugin plugin;
    private final LibraryService libraryService;
    private final ShelfMarkerService shelfMarkerService;
    private final BadgeService badgeService;

    public ShelfDestroyListener(
            final CuratedShelvesPlugin plugin,
            final LibraryService libraryService,
            final ShelfMarkerService shelfMarkerService,
            final BadgeService badgeService
    ) {
        this.plugin = plugin;
        this.libraryService = libraryService;
        this.shelfMarkerService = shelfMarkerService;
        this.badgeService = badgeService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        destroyShelf(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(final BlockBurnEvent event) {
        destroyShelf(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        for (Block block : new ArrayList<>(event.blockList())) {
            destroyShelf(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        for (Block block : new ArrayList<>(event.blockList())) {
            destroyShelf(block);
        }
    }

    private void destroyShelf(final Block block) {
        if (!this.shelfMarkerService.isMarked(block)) {
            return;
        }
        final UUID shelfId = this.shelfMarkerService.shelfId(block).orElse(null);
        if (shelfId == null || this.libraryService.shelfById(shelfId).isEmpty()) {
            if (shelfId != null) {
                this.badgeService.removeBadge(block, shelfId);
            }
            this.shelfMarkerService.unmark(block);
            return;
        }

        final var snapshot = this.libraryService.snapshotIfPresent(shelfId);
        if (snapshot.isEmpty()) {
            this.badgeService.removeBadge(block, shelfId);
            this.shelfMarkerService.unmark(block);
            return;
        }
        this.shelfMarkerService.unmark(block);
        this.badgeService.removeBadge(block, shelfId);
        spillBooks(block, snapshot.get());
        this.libraryService.deleteShelf(
                shelfId,
                () -> { },
                throwable -> this.plugin.getLogger().log(Level.SEVERE, "Failed to delete destroyed Library Shelf data", throwable)
        );
    }

    private void spillBooks(final Block block, final LibraryShelfSnapshot snapshot) {
        for (LibraryBook book : snapshot.booksBySlot().values()) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5D, 0.5D, 0.5D), BookCodec.deserializeItem(book.serializedItem()));
        }
    }
}
