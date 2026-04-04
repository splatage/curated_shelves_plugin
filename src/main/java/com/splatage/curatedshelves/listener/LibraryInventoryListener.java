package com.splatage.curatedshelves.listener;

import com.splatage.curatedshelves.data.LibraryBook;
import com.splatage.curatedshelves.data.LibraryShelf;
import com.splatage.curatedshelves.gui.LibraryMenuHolder;
import com.splatage.curatedshelves.service.LibraryService;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

public final class LibraryInventoryListener implements Listener {
    private final LibraryService libraryService;

    public LibraryInventoryListener(final LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof LibraryMenuHolder holder)) {
            return;
        }

        final Inventory topInventory = event.getView().getTopInventory();
        final Optional<LibraryShelf> shelfOptional = this.libraryService.findShelfById(holder.shelfId());
        if (shelfOptional.isEmpty()) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }
        final LibraryShelf shelf = shelfOptional.get();

        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory().equals(topInventory)) {
            event.setCancelled(true);
            handleTopInventoryClick(event, player, shelf, topInventory);
            return;
        }

        if (event.isShiftClick()) {
            final ItemStack currentItem = event.getCurrentItem();
            if (currentItem == null || currentItem.getType() != Material.WRITTEN_BOOK) {
                return;
            }
            final int emptySlot = firstEmptySlot(shelf, topInventory.getSize());
            if (emptySlot == -1) {
                event.setCancelled(true);
                player.sendMessage("That Library Shelf is full.");
                return;
            }
            if (!this.libraryService.canDeposit(player, currentItem)) {
                event.setCancelled(true);
                player.sendMessage(this.libraryService.depositDeniedMessage());
                return;
            }
            if (this.libraryService.depositBook(shelf, emptySlot, player, currentItem)) {
                currentItem.setAmount(currentItem.getAmount() - 1);
                this.libraryService.openLibrary(player, shelf);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof LibraryMenuHolder)) {
            return;
        }
        for (final int rawSlot : event.getRawSlots()) {
            if (rawSlot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleTopInventoryClick(
            final InventoryClickEvent event,
            final Player player,
            final LibraryShelf shelf,
            final Inventory topInventory
    ) {
        final int slot = event.getSlot();
        if (slot < 0 || slot >= topInventory.getSize()) {
            return;
        }

        final Optional<LibraryBook> bookOptional = shelf.getBook(slot);
        if (bookOptional.isPresent()) {
            final LibraryBook book = bookOptional.get();
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                if (!this.libraryService.canRemove(player, book)) {
                    player.sendMessage("You may read that book, but you cannot remove it.");
                    return;
                }
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("You need a free inventory slot to remove that book.");
                    return;
                }
                this.libraryService.removeBook(player, shelf.shelfId(), slot).ifPresent(removed -> {
                    player.getInventory().addItem(removed.item().clone());
                    this.libraryService.openLibrary(player, shelf);
                });
                return;
            }
            this.libraryService.openBook(player, book);
            return;
        }

        final ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() == Material.AIR) {
            return;
        }
        if (!this.libraryService.canDeposit(player, cursor)) {
            player.sendMessage(this.libraryService.depositDeniedMessage());
            return;
        }
        if (this.libraryService.depositBook(shelf, slot, player, cursor)) {
            cursor.setAmount(cursor.getAmount() - 1);
            this.libraryService.openLibrary(player, shelf);
        }
    }

    private int firstEmptySlot(final LibraryShelf shelf, final int size) {
        for (int slot = 0; slot < size; slot++) {
            if (shelf.getBook(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }
}
