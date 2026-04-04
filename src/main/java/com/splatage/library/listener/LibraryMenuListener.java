package com.splatage.library.listener;

import com.splatage.library.config.LibraryConfig;
import com.splatage.library.domain.LibraryBookRecord;
import com.splatage.library.domain.LibraryShelfRecord;
import com.splatage.library.gui.BookActionMenuHolder;
import com.splatage.library.gui.LibraryMenuHolder;
import com.splatage.library.gui.MenuFactory;
import com.splatage.library.platform.SchedulerFacade;
import com.splatage.library.service.LibraryService;
import com.splatage.library.util.BookItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class LibraryMenuListener implements Listener {
    private final LibraryService libraryService;
    private final MenuFactory menuFactory;
    private final LibraryConfig config;
    private final SchedulerFacade scheduler;

    public LibraryMenuListener(
            final LibraryService libraryService,
            final MenuFactory menuFactory,
            final LibraryConfig config,
            final SchedulerFacade scheduler
    ) {
        this.libraryService = libraryService;
        this.menuFactory = menuFactory;
        this.config = config;
        this.scheduler = scheduler;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        final InventoryView view = event.getView();
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (view.getTopInventory().getHolder() instanceof LibraryMenuHolder holder) {
            this.handleLibraryMenu(event, player, holder);
            return;
        }

        if (view.getTopInventory().getHolder() instanceof BookActionMenuHolder holder) {
            this.handleBookActionMenu(event, player, holder);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof LibraryMenuHolder) {
            for (final int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void handleLibraryMenu(final InventoryClickEvent event, final Player player, final LibraryMenuHolder holder) {
        final int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() < topSize) {
            event.setCancelled(true);
            final int slot = event.getRawSlot();
            final LibraryBookRecord bookRecord = this.libraryService.bookAt(holder.shelfId(), slot);
            if (bookRecord != null) {
                if (this.libraryService.canRemove(player, bookRecord, this.config)) {
                    player.openInventory(this.menuFactory.actionMenu(bookRecord));
                } else {
                    this.readBook(player, bookRecord);
                }
                return;
            }

            final ItemStack cursor = event.getCursor();
            if (BookItems.isWrittenBook(cursor)) {
                this.depositIntoSlot(player, holder.shelfId(), slot, cursor);
            }
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            if (!BookItems.isWrittenBook(event.getCurrentItem())) {
                return;
            }
            final int slot = this.libraryService.firstEmptySlot(holder.shelfId(), this.config.slotCount());
            if (slot == -1) {
                player.sendMessage(Component.text("That library shelf is full.", NamedTextColor.RED));
                return;
            }
            this.depositFromInventory(event, player, holder.shelfId(), slot, event.getCurrentItem());
        }
    }

    private void handleBookActionMenu(final InventoryClickEvent event, final Player player, final BookActionMenuHolder holder) {
        event.setCancelled(true);
        final LibraryBookRecord bookRecord = this.libraryService.bookAt(holder.shelfId(), holder.slotIndex());
        if (bookRecord == null) {
            player.closeInventory();
            return;
        }

        if (event.getRawSlot() == 3) {
            this.readBook(player, bookRecord);
            return;
        }
        if (event.getRawSlot() == 5) {
            final LibraryService.RemoveResult result = this.libraryService.removeBook(holder.shelfId(), holder.slotIndex(), player, this.config);
            switch (result) {
                case REMOVED -> {
                    player.sendMessage(Component.text("Removed book from the shelf.", NamedTextColor.GREEN));
                    this.refreshLibraryMenu(player, holder.shelfId());
                }
                case NO_SPACE -> player.sendMessage(Component.text("You need an empty inventory slot to remove that book.", NamedTextColor.RED));
                case NOT_ALLOWED -> player.sendMessage(Component.text("You cannot remove that book.", NamedTextColor.RED));
                case NO_BOOK -> player.closeInventory();
            }
        }
    }

    private void depositIntoSlot(final Player player, final UUID shelfId, final int slot, final ItemStack cursor) {
        final ItemStack single = cursor.clone();
        single.setAmount(1);
        final LibraryService.StoreResult result = this.libraryService.storeBook(shelfId, slot, single, player, this.config);
        switch (result) {
            case STORED -> {
                cursor.setAmount(cursor.getAmount() - 1);
                player.setItemOnCursor(cursor.getAmount() <= 0 ? null : cursor);
                this.refreshLibraryMenu(player, shelfId);
            }
            case AUTHOR_MISMATCH -> player.sendMessage(Component.text("Only the book's author may shelve that book here.", NamedTextColor.RED));
            case INVALID_ITEM -> player.sendMessage(Component.text("Only written books can be stored in a library shelf.", NamedTextColor.RED));
            case SLOT_OCCUPIED -> player.sendMessage(Component.text("That slot is already occupied.", NamedTextColor.RED));
            case INVALID_SLOT, NO_SHELF -> player.sendMessage(Component.text("That shelf is not available right now.", NamedTextColor.RED));
        }
    }

    private void depositFromInventory(final InventoryClickEvent event, final Player player, final UUID shelfId, final int slot, final ItemStack inventoryItem) {
        final ItemStack single = inventoryItem.clone();
        single.setAmount(1);
        final LibraryService.StoreResult result = this.libraryService.storeBook(shelfId, slot, single, player, this.config);
        switch (result) {
            case STORED -> {
                final int remaining = inventoryItem.getAmount() - 1;
                if (remaining <= 0) {
                    event.setCurrentItem(null);
                } else {
                    final ItemStack updated = inventoryItem.clone();
                    updated.setAmount(remaining);
                    event.setCurrentItem(updated);
                }
                this.refreshLibraryMenu(player, shelfId);
            }
            case AUTHOR_MISMATCH -> player.sendMessage(Component.text("Only the book's author may shelve that book here.", NamedTextColor.RED));
            case INVALID_ITEM -> player.sendMessage(Component.text("Only written books can be stored in a library shelf.", NamedTextColor.RED));
            case SLOT_OCCUPIED -> player.sendMessage(Component.text("That slot is already occupied.", NamedTextColor.RED));
            case INVALID_SLOT, NO_SHELF -> player.sendMessage(Component.text("That shelf is not available right now.", NamedTextColor.RED));
        }
    }

    private void refreshLibraryMenu(final Player player, final UUID shelfId) {
        final LibraryShelfRecord shelf = this.libraryService.shelf(shelfId).orElse(null);
        if (shelf == null) {
            player.closeInventory();
            return;
        }
        this.scheduler.executePlayer(player, audience -> audience.openInventory(this.menuFactory.libraryMenu(shelf, this.libraryService.booksFor(shelfId), this.config)));
    }

    private void readBook(final Player player, final LibraryBookRecord bookRecord) {
        player.closeInventory();
        this.scheduler.executePlayerDelayed(player, 1L, audience -> audience.openBook(bookRecord.item().clone()));
    }
}
