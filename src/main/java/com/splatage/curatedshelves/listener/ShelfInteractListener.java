package com.splatage.curatedshelves.listener;

import com.splatage.curatedshelves.service.LibraryService;
import com.splatage.curatedshelves.util.LibraryItems;
import com.splatage.curatedshelves.util.PdcKeys;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ShelfInteractListener implements Listener {
    private final LibraryService libraryService;
    private final PdcKeys keys;

    public ShelfInteractListener(final LibraryService libraryService, final PdcKeys keys) {
        this.libraryService = libraryService;
        this.keys = keys;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack handItem = event.getItem();
        if (LibraryItems.isLibrariansSeal(handItem, this.keys)) {
            if (clickedBlock.getType() != Material.CHISELED_BOOKSHELF) {
                return;
            }
            event.setCancelled(true);
            if (this.libraryService.isLibraryShelf(clickedBlock)) {
                player.sendMessage("That shelf is already marked as a Library Shelf.");
                return;
            }
            this.libraryService.markShelf(clickedBlock).ifPresentOrElse(shelf -> {
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && handItem != null) {
                    handItem.setAmount(handItem.getAmount() - 1);
                }
                player.sendMessage("Library Shelf created.");
            }, () -> player.sendMessage("That block cannot be marked as a Library Shelf."));
            return;
        }

        if (!this.libraryService.isLibraryShelf(clickedBlock)) {
            return;
        }
        if (!player.hasPermission("curatedshelves.use")) {
            player.sendMessage("You do not have permission to use library shelves.");
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        this.libraryService.findShelf(clickedBlock).ifPresent(shelf -> {
            if (this.libraryService.shelfHasHiddenBooks(shelf)) {
                player.sendMessage(this.libraryService.hiddenBooksMessage());
            }
            this.libraryService.openLibrary(player, shelf);
        });
    }
}
