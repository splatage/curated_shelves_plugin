package com.splatage.curatedshelves.listener;

import com.splatage.curatedshelves.service.LibraryService;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

public final class ShelfDestroyListener implements Listener {
    private final LibraryService libraryService;

    public ShelfDestroyListener(final LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (this.libraryService.isLibraryShelf(block)) {
            this.libraryService.spillAndDeleteShelf(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(final BlockBurnEvent event) {
        final Block block = event.getBlock();
        if (this.libraryService.isLibraryShelf(block)) {
            this.libraryService.spillAndDeleteShelf(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        for (final Block block : event.blockList()) {
            if (this.libraryService.isLibraryShelf(block)) {
                this.libraryService.spillAndDeleteShelf(block);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        for (final Block block : event.blockList()) {
            if (this.libraryService.isLibraryShelf(block)) {
                this.libraryService.spillAndDeleteShelf(block);
            }
        }
    }
}
