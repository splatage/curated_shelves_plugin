package com.splatage.curatedshelves.listener;

import com.splatage.curatedshelves.CuratedShelvesPlugin;
import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.domain.LocationKey;
import com.splatage.curatedshelves.gui.LibraryViews;
import com.splatage.curatedshelves.service.BadgeService;
import com.splatage.curatedshelves.service.LibraryService;
import com.splatage.curatedshelves.service.ShelfMarkerService;
import com.splatage.curatedshelves.util.LibraryItems;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class ShelfInteractListener implements Listener {
    private final CuratedShelvesPlugin plugin;
    private final LibraryService libraryService;
    private final ShelfMarkerService shelfMarkerService;
    private final BadgeService badgeService;

    public ShelfInteractListener(
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null || !this.shelfMarkerService.isEligibleBlock(block)) {
            return;
        }

        final ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
        if (LibraryItems.isLibrariansSeal(this.plugin.pdcKeys(), heldItem)) {
            event.setCancelled(true);
            handleSealUse(event.getPlayer(), block);
            return;
        }

        if (!this.shelfMarkerService.isMarked(block)) {
            return;
        }

        final Optional<java.util.UUID> shelfId = this.shelfMarkerService.shelfId(block);
        if (shelfId.isEmpty() || this.libraryService.shelfById(shelfId.get()).isEmpty() || this.libraryService.isShelfPendingRemoval(shelfId.get())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("This Library Shelf is unavailable.");
            return;
        }
        final var snapshot = this.libraryService.snapshotIfPresent(shelfId.get());
        if (snapshot.isEmpty()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("This Library Shelf is unavailable.");
            return;
        }
        if (this.shelfMarkerService.hasPhysicalContents(block)
                && snapshot.get().booksBySlot().isEmpty()) {
            return;
        }

        event.setCancelled(true);
        if (!event.getPlayer().hasPermission("curatedshelves.use")
                && !event.getPlayer().hasPermission("curatedshelves.admin.edit")) {
            event.getPlayer().sendMessage("You do not have permission to use Library Shelves.");
            return;
        }
        LibraryViews.openShelfMenu(event.getPlayer(), snapshot.get());
    }

    private void handleSealUse(final Player player, final Block block) {
        if (this.shelfMarkerService.isMarked(block)) {
            player.sendMessage("That shelf is already marked as a Library Shelf.");
            return;
        }
        if (this.shelfMarkerService.hasPhysicalContents(block)) {
            player.sendMessage("That shelf must be empty before it can become a Library Shelf.");
            return;
        }

        final LibraryShelf shelf = this.libraryService.newShelf(LocationKey.fromLocation(block.getLocation()), this.plugin.pluginConfig().rows(), player);
        this.libraryService.createShelf(
                shelf,
                () -> this.plugin.schedulerFacade().runAtLocation(block.getLocation(), () -> {
                    if (!this.shelfMarkerService.isEligibleBlock(block)) {
                        this.libraryService.deleteUnboundShelf(shelf.shelfId(), () -> { }, deleteFailure ->
                                this.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to clean up unbound Library Shelf", deleteFailure)
                        );
                        this.plugin.schedulerFacade().runForPlayer(player, () -> player.sendMessage("The target shelf changed before it could be marked."));
                        return;
                    }
                    try {
                        this.shelfMarkerService.mark(block, shelf.shelfId());
                        this.badgeService.ensureBadge(block, shelf);
                        if (!this.libraryService.activateCreatedShelf(shelf.shelfId())) {
                            this.shelfMarkerService.unmark(block);
                            this.badgeService.removeBadge(block, shelf.shelfId());
                            this.libraryService.deleteUnboundShelf(shelf.shelfId(), () -> { }, deleteFailure ->
                                    this.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to clean up unbound Library Shelf", deleteFailure)
                            );
                            this.plugin.schedulerFacade().runForPlayer(player, () -> player.sendMessage("The Library Shelf could not be finalized."));
                            return;
                        }
                        this.plugin.schedulerFacade().runForPlayer(player, () -> {
                            if (player.getGameMode() != GameMode.CREATIVE) {
                                consumeOneSeal(player);
                            }
                            player.sendMessage("Library Shelf created.");
                        });
                    } catch (final Throwable throwable) {
                        this.shelfMarkerService.unmark(block);
                        this.badgeService.removeBadge(block, shelf.shelfId());
                        this.libraryService.deleteUnboundShelf(shelf.shelfId(), () -> { }, deleteFailure ->
                                this.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to clean up unbound Library Shelf", deleteFailure)
                        );
                        this.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to finalize Library Shelf binding", throwable);
                        this.plugin.schedulerFacade().runForPlayer(player, () -> player.sendMessage("Failed to create the Library Shelf."));
                    }
                }),
                throwable -> this.plugin.schedulerFacade().runForPlayer(player, () -> {
                    this.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to create Library Shelf", throwable);
                    player.sendMessage("Failed to create the Library Shelf.");
                })
        );
    }

    private void consumeOneSeal(final Player player) {
        final ItemStack current = player.getInventory().getItemInMainHand();
        if (!LibraryItems.isLibrariansSeal(this.plugin.pdcKeys(), current)) {
            return;
        }
        if (current.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
            return;
        }
        current.setAmount(current.getAmount() - 1);
        player.getInventory().setItemInMainHand(current);
    }
}
