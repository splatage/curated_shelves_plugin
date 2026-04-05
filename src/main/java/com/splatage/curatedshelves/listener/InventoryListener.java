package com.splatage.curatedshelves.listener;

import com.splatage.curatedshelves.CuratedShelvesPlugin;
import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.gui.BookActionMenu;
import com.splatage.curatedshelves.gui.BookActionMenuHolder;
import com.splatage.curatedshelves.gui.LibraryMenuHolder;
import com.splatage.curatedshelves.gui.LibraryViews;
import com.splatage.curatedshelves.gui.ShelfBrowserMenu;
import com.splatage.curatedshelves.gui.ShelfBrowserMenuHolder;
import com.splatage.curatedshelves.service.LibraryService;
import com.splatage.curatedshelves.util.BookCodec;
import com.splatage.curatedshelves.util.LibraryItems;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class InventoryListener implements Listener {
    private final CuratedShelvesPlugin plugin;
    private final LibraryService libraryService;
    private final Set<UUID> pendingPlayers = ConcurrentHashMap.newKeySet();

    public InventoryListener(final CuratedShelvesPlugin plugin, final LibraryService libraryService) {
        this.plugin = plugin;
        this.libraryService = libraryService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        final Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() instanceof LibraryMenuHolder holder) {
            handleLibraryMenuClick(event, holder);
            return;
        }
        if (topInventory.getHolder() instanceof BookActionMenuHolder holder) {
            handleBookActionClick(event, holder);
            return;
        }
        if (topInventory.getHolder() instanceof ShelfBrowserMenuHolder holder) {
            handleShelfBrowserClick(event, holder);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        final Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof LibraryMenuHolder)
                && !(topInventory.getHolder() instanceof BookActionMenuHolder)
                && !(topInventory.getHolder() instanceof ShelfBrowserMenuHolder)) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topInventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleShelfBrowserClick(final InventoryClickEvent event, final ShelfBrowserMenuHolder holder) {
        final Player player = (Player) event.getWhoClicked();
        final Inventory topInventory = event.getView().getTopInventory();
        if (!player.hasPermission("curatedshelves.admin.browse")) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage("You do not have permission to browse Curated Shelves.");
            return;
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (event.getRawSlot() >= topInventory.getSize()) {
            return;
        }
        event.setCancelled(true);

        final int rawSlot = event.getRawSlot();
        if (rawSlot == ShelfBrowserMenu.PREVIOUS_PAGE_SLOT && holder.page() > 0) {
            LibraryViews.openShelfBrowserMenu(player, this.libraryService.allShelfSnapshotsSorted(), holder.page() - 1);
            return;
        }
        if (rawSlot == ShelfBrowserMenu.NEXT_PAGE_SLOT && holder.page() + 1 < holder.totalPages()) {
            LibraryViews.openShelfBrowserMenu(player, this.libraryService.allShelfSnapshotsSorted(), holder.page() + 1);
            return;
        }
        final UUID shelfId = holder.shelfIdAt(rawSlot);
        if (shelfId == null) {
            return;
        }
        final var snapshot = this.libraryService.snapshotIfPresent(shelfId);
        if (snapshot.isEmpty()) {
            player.sendMessage("That Library Shelf is no longer available.");
            LibraryViews.openShelfBrowserMenu(player, this.libraryService.allShelfSnapshotsSorted(), holder.page());
            return;
        }
        LibraryViews.openShelfMenu(player, snapshot.get());
    }

    private void handleLibraryMenuClick(final InventoryClickEvent event, final LibraryMenuHolder holder) {
        final Player player = (Player) event.getWhoClicked();
        final Inventory topInventory = event.getView().getTopInventory();
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (event.getRawSlot() >= topInventory.getSize()) {
            return;
        }
        event.setCancelled(true);

        if (!canOpenLibraryMenu(player)) {
            player.sendMessage("You do not have permission to use Library Shelves.");
            return;
        }
        if (this.pendingPlayers.contains(player.getUniqueId())) {
            player.sendMessage("Please wait for the current library action to finish.");
            return;
        }

        final UUID shelfId = holder.shelfId();
        final boolean canUseShelves = player.hasPermission("curatedshelves.use");
        if (this.libraryService.shelfById(shelfId).isEmpty() || this.libraryService.isShelfPendingRemoval(shelfId)) {
            player.closeInventory();
            player.sendMessage("That Library Shelf is no longer available.");
            return;
        }

        final int clickedSlot = event.getRawSlot();
        final ItemStack cursor = event.getCursor();
        final LibraryBook existingBook = this.libraryService.bookAt(shelfId, clickedSlot).orElse(null);
        if (existingBook != null) {
            if (this.libraryService.canRemoveBook(player, existingBook, this.plugin.pluginConfig())) {
                LibraryViews.openBookActionMenu(player, shelfId, existingBook.bookId());
                return;
            }
            player.openBook(BookCodec.deserializeItem(existingBook.serializedItem()));
            return;
        }

        if (!LibraryItems.isSupportedBook(cursor)) {
            return;
        }
        if (!canUseShelves) {
            player.sendMessage("You do not have permission to shelve books.");
            return;
        }
        if (!this.libraryService.canDepositBook(player, cursor, this.plugin.pluginConfig())) {
            player.sendMessage("You may not shelve this book.");
            return;
        }

        final ItemStack depositItem = cursor.clone();
        depositItem.setAmount(1);
        final Location returnLocation = player.getLocation().clone();
        decrementCursor(event);
        this.pendingPlayers.add(player.getUniqueId());
        final LibraryBook newBook = this.libraryService.newBook(shelfId, clickedSlot, depositItem, player);
        this.libraryService.storeBook(
                newBook,
                () -> this.plugin.schedulerFacade().runForPlayer(player, () -> {
                    this.pendingPlayers.remove(player.getUniqueId());
                    refreshLibraryMenu(player, shelfId);
                    player.sendMessage("Book shelved.");
                }, () -> this.pendingPlayers.remove(player.getUniqueId())),
                throwable -> this.plugin.schedulerFacade().runForPlayer(player, () -> {
                    this.pendingPlayers.remove(player.getUniqueId());
                    restoreBook(player, depositItem);
                    this.plugin.getLogger().log(Level.SEVERE, "Failed to store library book", throwable);
                    player.sendMessage("Failed to shelve the book.");
                    refreshLibraryMenu(player, shelfId);
                }, () -> {
                    this.pendingPlayers.remove(player.getUniqueId());
                    this.plugin.getLogger().log(Level.SEVERE, "Failed to store library book after player retired", throwable);
                    dropBook(returnLocation, depositItem);
                })
        );
    }

    private void handleBookActionClick(final InventoryClickEvent event, final BookActionMenuHolder holder) {
        final Player player = (Player) event.getWhoClicked();
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        event.setCancelled(true);
        if (!canOpenLibraryMenu(player)) {
            player.sendMessage("You do not have permission to use Library Shelves.");
            return;
        }
        if (this.pendingPlayers.contains(player.getUniqueId())) {
            player.sendMessage("Please wait for the current library action to finish.");
            return;
        }
        if (this.libraryService.shelfById(holder.shelfId()).isEmpty() || this.libraryService.isShelfPendingRemoval(holder.shelfId())) {
            player.closeInventory();
            player.sendMessage("That Library Shelf is no longer available.");
            return;
        }

        final LibraryBook book = this.libraryService.bookById(holder.bookId()).orElse(null);
        if (book == null) {
            player.closeInventory();
            player.sendMessage("That book is no longer available.");
            return;
        }

        if (event.getRawSlot() == BookActionMenu.READ_SLOT) {
            player.openBook(BookCodec.deserializeItem(book.serializedItem()));
            return;
        }
        if (event.getRawSlot() != BookActionMenu.REMOVE_SLOT) {
            return;
        }
        if (!this.libraryService.canRemoveBook(player, book, this.plugin.pluginConfig())) {
            player.sendMessage("You may not remove that book.");
            return;
        }

        final ItemStack returnedBook = BookCodec.deserializeItem(book.serializedItem());
        final Location returnLocation = player.getLocation().clone();
        this.pendingPlayers.add(player.getUniqueId());
        this.libraryService.removeBook(
                book,
                () -> this.plugin.schedulerFacade().runForPlayer(player, () -> {
                    this.pendingPlayers.remove(player.getUniqueId());
                    restoreBook(player, returnedBook);
                    refreshLibraryMenu(player, holder.shelfId());
                    player.sendMessage("Book removed.");
                }, () -> {
                    this.pendingPlayers.remove(player.getUniqueId());
                    dropBook(returnLocation, returnedBook);
                }),
                throwable -> this.plugin.schedulerFacade().runForPlayer(player, () -> {
                    this.pendingPlayers.remove(player.getUniqueId());
                    this.plugin.getLogger().log(Level.SEVERE, "Failed to remove library book", throwable);
                    player.sendMessage("Failed to remove the book.");
                    refreshLibraryMenu(player, holder.shelfId());
                }, () -> {
                    this.pendingPlayers.remove(player.getUniqueId());
                    this.plugin.getLogger().log(Level.SEVERE, "Failed to remove library book after player retired", throwable);
                })
        );
    }

    private boolean canOpenLibraryMenu(final Player player) {
        return player.hasPermission("curatedshelves.use")
                || player.hasPermission("curatedshelves.admin.browse");
    }

    private void refreshLibraryMenu(final Player player, final UUID shelfId) {
        final var snapshot = this.libraryService.snapshotIfPresent(shelfId);
        if (snapshot.isEmpty()) {
            player.closeInventory();
            return;
        }
        LibraryViews.openShelfMenu(player, snapshot.get());
    }

    private void decrementCursor(final InventoryClickEvent event) {
        final ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getAmount() <= 0) {
            return;
        }
        if (cursor.getAmount() == 1) {
            event.getView().setCursor(null);
            return;
        }
        final ItemStack remaining = cursor.clone();
        remaining.setAmount(remaining.getAmount() - 1);
        event.getView().setCursor(remaining);
    }

    private void restoreBook(final Player player, final ItemStack itemStack) {
        final java.util.Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private void dropBook(final Location location, final ItemStack itemStack) {
        final World world = location.getWorld();
        if (world == null) {
            this.plugin.getLogger().warning("Unable to return a library book because the target world is unavailable.");
            return;
        }
        this.plugin.schedulerFacade().runAtLocation(location, () -> world.dropItemNaturally(location, itemStack));
    }
}
