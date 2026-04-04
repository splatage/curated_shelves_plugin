package com.splatage.curatedshelves.service;

import com.splatage.curatedshelves.config.PluginSettings;
import com.splatage.curatedshelves.data.LibraryBook;
import com.splatage.curatedshelves.data.LibraryShelf;
import com.splatage.curatedshelves.data.ShelfKey;
import com.splatage.curatedshelves.data.ShelfStore;
import com.splatage.curatedshelves.gui.LibraryMenuHolder;
import com.splatage.curatedshelves.platform.SchedulerFacade;
import com.splatage.curatedshelves.util.PdcKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class LibraryService {
    private final Plugin plugin;
    private final SchedulerFacade schedulerFacade;
    private final ShelfStore shelfStore;
    private final BadgeService badgeService;
    private final PdcKeys keys;
    private PluginSettings settings;

    public LibraryService(
            final Plugin plugin,
            final SchedulerFacade schedulerFacade,
            final ShelfStore shelfStore,
            final BadgeService badgeService,
            final PdcKeys keys,
            final PluginSettings settings
    ) {
        this.plugin = plugin;
        this.schedulerFacade = schedulerFacade;
        this.shelfStore = shelfStore;
        this.badgeService = badgeService;
        this.keys = keys;
        this.settings = settings;
    }

    public void updateSettings(final PluginSettings settings) {
        this.settings = settings;
    }

    public boolean isLibraryShelf(final Block block) {
        if (block.getType() != Material.CHISELED_BOOKSHELF) {
            return false;
        }
        if (!(block.getState() instanceof TileState tileState)) {
            return false;
        }
        return tileState.getPersistentDataContainer().has(this.keys.shelfMarker(), PersistentDataType.BYTE)
                && tileState.getPersistentDataContainer().has(this.keys.shelfId(), PersistentDataType.STRING);
    }

    public Optional<LibraryShelf> findShelf(final Block block) {
        return this.shelfStore.findByKey(ShelfKey.fromBlock(block));
    }

    public Optional<LibraryShelf> markShelf(final Block block) {
        if (block.getType() != Material.CHISELED_BOOKSHELF) {
            return Optional.empty();
        }
        if (!(block.getState() instanceof TileState tileState)) {
            return Optional.empty();
        }
        final LibraryShelf shelf = this.shelfStore.createShelf(ShelfKey.fromBlock(block));
        final PersistentDataContainer persistentDataContainer = tileState.getPersistentDataContainer();
        persistentDataContainer.set(this.keys.shelfMarker(), PersistentDataType.BYTE, (byte) 1);
        persistentDataContainer.set(this.keys.shelfId(), PersistentDataType.STRING, shelf.shelfId());
        tileState.update(true, false);
        this.badgeService.spawnOrRefreshBadge(shelf);
        return Optional.of(shelf);
    }

    public boolean unmarkShelfIfEmpty(final Block block) {
        final Optional<LibraryShelf> shelfOptional = findShelf(block);
        if (shelfOptional.isEmpty()) {
            return false;
        }
        if (!this.shelfStore.removeShelfIfEmpty(ShelfKey.fromBlock(block))) {
            return false;
        }
        if (block.getState() instanceof TileState tileState) {
            final PersistentDataContainer persistentDataContainer = tileState.getPersistentDataContainer();
            persistentDataContainer.remove(this.keys.shelfMarker());
            persistentDataContainer.remove(this.keys.shelfId());
            tileState.update(true, false);
        }
        this.badgeService.removeBadge(shelfOptional.get());
        return true;
    }

    public void openLibrary(final Player player, final LibraryShelf shelf) {
        final int size = this.settings.size();
        final LibraryMenuHolder holder = new LibraryMenuHolder(shelf.shelfId());
        final Inventory inventory = Bukkit.createInventory(holder, size, "Library Shelf");
        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            shelf.getBook(slot).ifPresent(book -> inventory.setItem(slot, buildDisplayItem(book, canRemove(player, book))));
        }

        this.schedulerFacade.executeEntity(player, () -> player.openInventory(inventory));
    }

    public boolean shelfHasHiddenBooks(final LibraryShelf shelf) {
        return shelf.highestUsedSlot() >= this.settings.size();
    }

    public String hiddenBooksMessage() {
        return "This shelf contains books outside the current configured GUI size. Increase rows in config.yml.";
    }

    public boolean canDeposit(final Player player, final ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.WRITTEN_BOOK) {
            return false;
        }
        if (!this.settings.authorMustDeposit()) {
            return true;
        }
        if (!(itemStack.getItemMeta() instanceof BookMeta bookMeta)) {
            return false;
        }
        final String author = normalizeText(bookMeta.getAuthor(), "Unknown");
        return author.equalsIgnoreCase(player.getName());
    }

    public String depositDeniedMessage() {
        return this.settings.authorMustDeposit()
                ? "Only the book's author may shelve that book here."
                : "Only written books can be shelved here.";
    }

    public boolean depositBook(final LibraryShelf shelf, final int slotIndex, final Player player, final ItemStack itemStack) {
        if (slotIndex < 0 || slotIndex >= this.settings.size()) {
            return false;
        }
        if (!canDeposit(player, itemStack)) {
            return false;
        }
        if (shelf.getBook(slotIndex).isPresent()) {
            return false;
        }
        final ItemStack stored = itemStack.clone();
        stored.setAmount(1);
        final BookMeta bookMeta = (BookMeta) stored.getItemMeta();
        final String title = normalizeText(bookMeta.getTitle(), "Untitled Book");
        final String author = normalizeText(bookMeta.getAuthor(), "Unknown");
        final LibraryBook book = new LibraryBook(
                slotIndex,
                stored,
                title,
                author,
                player.getUniqueId().toString(),
                player.getName()
        );
        return this.shelfStore.putBook(shelf.shelfId(), book);
    }

    public Optional<LibraryBook> findBook(final String shelfId, final int slotIndex) {
        return this.shelfStore.findByShelfId(shelfId).flatMap(shelf -> shelf.getBook(slotIndex));
    }

    public boolean canRemove(final Player player, final LibraryBook book) {
        if (player.hasPermission("curatedshelves.librarian.remove.any")) {
            return true;
        }
        if (player.getUniqueId().toString().equals(book.shelvedByUuid())) {
            return true;
        }
        return this.settings.authorCanRemoveOwnBook() && book.author().equalsIgnoreCase(player.getName());
    }

    public Optional<LibraryBook> removeBook(final Player player, final String shelfId, final int slotIndex) {
        final Optional<LibraryBook> bookOptional = findBook(shelfId, slotIndex);
        if (bookOptional.isEmpty()) {
            return Optional.empty();
        }
        final LibraryBook book = bookOptional.get();
        if (!canRemove(player, book)) {
            return Optional.empty();
        }
        return this.shelfStore.removeBook(shelfId, slotIndex);
    }

    public void openBook(final Player player, final LibraryBook book) {
        this.schedulerFacade.executeEntity(player, () -> player.openBook(book.item().clone()));
    }

    public void spillAndDeleteShelf(final Block block) {
        final Optional<LibraryShelf> shelfOptional = findShelf(block);
        if (shelfOptional.isEmpty()) {
            return;
        }
        final LibraryShelf shelf = shelfOptional.get();
        final List<LibraryBook> removedBooks = this.shelfStore.removeShelf(ShelfKey.fromBlock(block));
        this.badgeService.removeBadge(shelf);
        if (block.getState() instanceof TileState tileState) {
            final PersistentDataContainer persistentDataContainer = tileState.getPersistentDataContainer();
            persistentDataContainer.remove(this.keys.shelfMarker());
            persistentDataContainer.remove(this.keys.shelfId());
            tileState.update(true, false);
        }
        final Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        for (final LibraryBook book : removedBooks) {
            final Item dropped = block.getWorld().dropItemNaturally(dropLocation, book.item().clone());
            dropped.setPickupDelay(10);
        }
    }

    public void rebuildVisibleBadges() {
        for (final LibraryShelf shelf : this.shelfStore.allShelves()) {
            final var world = Bukkit.getWorld(UUID.fromString(shelf.key().worldUid()));
            if (world == null) {
                continue;
            }
            if (!world.isChunkLoaded(shelf.key().x() >> 4, shelf.key().z() >> 4)) {
                continue;
            }
            final Block block = world.getBlockAt(shelf.key().x(), shelf.key().y(), shelf.key().z());
            if (block.getType() != Material.CHISELED_BOOKSHELF) {
                continue;
            }
            this.schedulerFacade.executeRegion(block.getLocation(), () -> this.badgeService.spawnOrRefreshBadge(shelf));
        }
    }

    public Optional<LibraryShelf> findShelfById(final String shelfId) {
        return this.shelfStore.findByShelfId(shelfId);
    }

    private ItemStack buildDisplayItem(final LibraryBook book, final boolean canRemove) {
        final ItemStack display = book.item().clone();
        final ItemMeta itemMeta = display.getItemMeta();
        itemMeta.setLore(buildLore(book, canRemove));
        display.setItemMeta(itemMeta);
        return display;
    }

    private List<String> buildLore(final LibraryBook book, final boolean canRemove) {
        final List<String> lore = new ArrayList<>();
        lore.add("§7Title: §f" + normalizeText(book.title(), "Untitled Book"));
        lore.add("§7Author: §f" + normalizeText(book.author(), "Unknown"));
        lore.add("§7Shelved by: §f" + normalizeText(book.shelvedByName(), "Unknown"));
        lore.add("");
        lore.add("§eLeft-click to read");
        if (canRemove) {
            lore.add("§cShift-click to remove");
        }
        return lore;
    }

    private String normalizeText(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
