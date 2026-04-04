package com.splatage.library.service;

import com.splatage.library.config.LibraryConfig;
import com.splatage.library.domain.BlockKey;
import com.splatage.library.domain.LibraryBookRecord;
import com.splatage.library.domain.LibraryShelfRecord;
import com.splatage.library.domain.LoadedLibraryState;
import com.splatage.library.persistence.SQLiteLibraryRepository;
import com.splatage.library.util.BookItems;
import com.splatage.library.util.PdcKeys;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LibraryService {
    private final SQLiteLibraryRepository repository;
    private final LibraryBadgeService badgeService;
    private final PdcKeys keys;
    private final Map<UUID, LibraryShelfRecord> shelvesById = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, LibraryBookRecord>> booksByShelfId = new ConcurrentHashMap<>();

    public LibraryService(
            final SQLiteLibraryRepository repository,
            final LibraryBadgeService badgeService,
            final PdcKeys keys
    ) {
        this.repository = repository;
        this.badgeService = badgeService;
        this.keys = keys;
    }

    public void loadInitialState() {
        final LoadedLibraryState state = this.repository.loadAll();
        for (final LibraryShelfRecord shelf : state.shelves()) {
            this.shelvesById.put(shelf.shelfId(), shelf);
            this.booksByShelfId.putIfAbsent(shelf.shelfId(), new ConcurrentHashMap<>());
        }
        for (final LibraryBookRecord book : state.books()) {
            this.booksByShelfId.computeIfAbsent(book.shelfId(), ignored -> new ConcurrentHashMap<>())
                    .put(book.slotIndex(), book);
        }
    }

    public boolean isLibraryShelf(final @NotNull Block block) {
        return this.readShelfId(block).isPresent();
    }

    public Optional<UUID> readShelfId(final @NotNull Block block) {
        if (block.getType() != Material.CHISELED_BOOKSHELF) {
            return Optional.empty();
        }
        if (!(block.getState() instanceof TileState tileState)) {
            return Optional.empty();
        }
        final PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        if (!pdc.has(this.keys.libraryShelf, PersistentDataType.BYTE)) {
            return Optional.empty();
        }
        final String shelfId = pdc.get(this.keys.shelfId, PersistentDataType.STRING);
        if (shelfId == null || shelfId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(shelfId));
    }

    public synchronized MarkResult markShelf(final @NotNull Block block) {
        if (block.getType() != Material.CHISELED_BOOKSHELF) {
            return MarkResult.INVALID_BLOCK;
        }
        if (this.isLibraryShelf(block)) {
            return MarkResult.ALREADY_MARKED;
        }
        if (!(block.getState() instanceof TileState tileState)) {
            return MarkResult.INVALID_BLOCK;
        }

        final long now = Instant.now().toEpochMilli();
        final UUID shelfId = UUID.randomUUID();
        final LibraryShelfRecord shelfRecord = new LibraryShelfRecord(shelfId, BlockKey.fromBlock(block), now, now);
        this.repository.upsertShelf(shelfRecord);

        final PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        pdc.set(this.keys.libraryShelf, PersistentDataType.BYTE, (byte) 1);
        pdc.set(this.keys.shelfId, PersistentDataType.STRING, shelfId.toString());
        tileState.update(true, false);

        this.shelvesById.put(shelfId, shelfRecord);
        this.booksByShelfId.putIfAbsent(shelfId, new ConcurrentHashMap<>());
        this.badgeService.ensureBadge(block, shelfId);
        return MarkResult.MARKED;
    }

    public synchronized UnmarkResult unmarkShelf(final @NotNull Block block) {
        final Optional<UUID> shelfIdOptional = this.readShelfId(block);
        if (shelfIdOptional.isEmpty()) {
            return UnmarkResult.NOT_MARKED;
        }
        final UUID shelfId = shelfIdOptional.get();
        if (!this.booksFor(shelfId).isEmpty()) {
            return UnmarkResult.NOT_EMPTY;
        }
        if (!(block.getState() instanceof TileState tileState)) {
            return UnmarkResult.NOT_MARKED;
        }

        this.repository.deleteShelf(shelfId);
        final PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        pdc.remove(this.keys.libraryShelf);
        pdc.remove(this.keys.shelfId);
        tileState.update(true, false);

        this.badgeService.removeBadge(block, shelfId);
        this.shelvesById.remove(shelfId);
        this.booksByShelfId.remove(shelfId);
        return UnmarkResult.UNMARKED;
    }

    public Optional<LibraryShelfRecord> resolveShelf(final @NotNull Block block) {
        final Optional<UUID> shelfIdOptional = this.readShelfId(block);
        if (shelfIdOptional.isEmpty()) {
            return Optional.empty();
        }
        final UUID shelfId = shelfIdOptional.get();
        final LibraryShelfRecord known = this.shelvesById.get(shelfId);
        if (known != null) {
            return Optional.of(known);
        }

        final long now = Instant.now().toEpochMilli();
        final LibraryShelfRecord reconstructed = new LibraryShelfRecord(shelfId, BlockKey.fromBlock(block), now, now);
        this.shelvesById.put(shelfId, reconstructed);
        this.booksByShelfId.putIfAbsent(shelfId, new ConcurrentHashMap<>());
        this.repository.upsertShelf(reconstructed);
        this.badgeService.ensureBadge(block, shelfId);
        return Optional.of(reconstructed);
    }


    public Optional<LibraryShelfRecord> shelf(final @NotNull UUID shelfId) {
        final LibraryShelfRecord shelf = this.shelvesById.get(shelfId);
        return shelf == null ? Optional.empty() : Optional.of(shelf);
    }

    public List<LibraryBookRecord> booksFor(final @NotNull UUID shelfId) {
        return this.booksByShelfId.getOrDefault(shelfId, Map.of()).values().stream()
                .sorted(Comparator.comparingInt(LibraryBookRecord::slotIndex))
                .map(this::copyBook)
                .toList();
    }

    public @Nullable LibraryBookRecord bookAt(final @NotNull UUID shelfId, final int slotIndex) {
        final LibraryBookRecord record = this.booksByShelfId.getOrDefault(shelfId, Map.of()).get(slotIndex);
        return record == null ? null : this.copyBook(record);
    }

    public int firstEmptySlot(final @NotNull UUID shelfId, final int slotCount) {
        final Map<Integer, LibraryBookRecord> books = this.booksByShelfId.getOrDefault(shelfId, Map.of());
        for (int slot = 0; slot < slotCount; slot++) {
            if (!books.containsKey(slot)) {
                return slot;
            }
        }
        return -1;
    }

    public synchronized StoreResult storeBook(
            final @NotNull UUID shelfId,
            final int slotIndex,
            final @NotNull ItemStack itemStack,
            final @NotNull Player player,
            final @NotNull LibraryConfig config
    ) {
        final LibraryShelfRecord shelf = this.shelvesById.get(shelfId);
        if (shelf == null) {
            return StoreResult.NO_SHELF;
        }
        if (slotIndex < 0 || slotIndex >= config.slotCount()) {
            return StoreResult.INVALID_SLOT;
        }
        if (!BookItems.isWrittenBook(itemStack)) {
            return StoreResult.INVALID_ITEM;
        }
        if (this.booksByShelfId.computeIfAbsent(shelfId, ignored -> new ConcurrentHashMap<>()).containsKey(slotIndex)) {
            return StoreResult.SLOT_OCCUPIED;
        }
        if (config.authorMustDeposit() && !BookItems.canDepositByAuthorRule(itemStack, player)) {
            return StoreResult.AUTHOR_MISMATCH;
        }

        final long now = Instant.now().toEpochMilli();
        final LibraryBookRecord bookRecord = new LibraryBookRecord(
                UUID.randomUUID(),
                shelfId,
                slotIndex,
                BookItems.singleCopy(itemStack),
                BookItems.titleOf(itemStack),
                BookItems.authorOf(itemStack),
                player.getUniqueId(),
                player.getName(),
                now,
                now
        );
        this.repository.upsertBook(bookRecord);
        this.booksByShelfId.get(shelfId).put(slotIndex, bookRecord);
        this.shelvesById.computeIfPresent(shelfId, (ignored, existing) -> existing.touch(now));
        final LibraryShelfRecord updatedShelf = this.shelvesById.get(shelfId);
        if (updatedShelf != null) {
            this.repository.upsertShelf(updatedShelf);
        }
        return StoreResult.STORED;
    }

    public synchronized RemoveResult removeBook(
            final @NotNull UUID shelfId,
            final int slotIndex,
            final @NotNull Player player,
            final @NotNull LibraryConfig config
    ) {
        final LibraryBookRecord bookRecord = this.booksByShelfId.getOrDefault(shelfId, Map.of()).get(slotIndex);
        if (bookRecord == null) {
            return RemoveResult.NO_BOOK;
        }
        if (!this.canRemove(player, bookRecord, config)) {
            return RemoveResult.NOT_ALLOWED;
        }
        if (player.getInventory().firstEmpty() == -1) {
            return RemoveResult.NO_SPACE;
        }
        player.getInventory().addItem(bookRecord.item().clone());
        this.booksByShelfId.getOrDefault(shelfId, Map.of()).remove(slotIndex);
        try {
            this.repository.deleteBook(bookRecord.bookId());
        } catch (final RuntimeException exception) {
            System.getLogger(LibraryService.class.getName())
                    .log(System.Logger.Level.ERROR, "Failed to delete removed library book from persistence", exception);
        }
        return RemoveResult.REMOVED;
    }

    public boolean canRemove(final @NotNull Player player, final @NotNull LibraryBookRecord bookRecord, final @NotNull LibraryConfig config) {
        if (player.hasPermission("library.librarian.remove.any")) {
            return true;
        }
        if (bookRecord.shelvedByUuid().equals(player.getUniqueId())) {
            return true;
        }
        return config.authorCanRemoveOwnBook() && bookRecord.author().equalsIgnoreCase(player.getName());
    }

    public synchronized List<ItemStack> spillShelf(final @NotNull Block block) {
        final Optional<UUID> shelfIdOptional = this.readShelfId(block);
        if (shelfIdOptional.isEmpty()) {
            return List.of();
        }
        final UUID shelfId = shelfIdOptional.get();
        final List<ItemStack> drops = new ArrayList<>();
        for (final LibraryBookRecord record : this.booksFor(shelfId)) {
            drops.add(record.item().clone());
        }

        this.badgeService.removeBadge(block, shelfId);
        this.shelvesById.remove(shelfId);
        this.booksByShelfId.remove(shelfId);
        try {
            this.repository.deleteShelf(shelfId);
        } catch (final RuntimeException exception) {
            System.getLogger(LibraryService.class.getName())
                    .log(System.Logger.Level.ERROR, "Failed to delete spilled library shelf from persistence", exception);
        }
        return drops;
    }

    public void ensureBadge(final @NotNull Block block) {
        this.resolveShelf(block).ifPresent(shelf -> this.badgeService.ensureBadge(block, shelf.shelfId()));
    }

    private LibraryBookRecord copyBook(final LibraryBookRecord record) {
        return new LibraryBookRecord(
                record.bookId(),
                record.shelfId(),
                record.slotIndex(),
                record.item().clone(),
                record.title(),
                record.author(),
                record.shelvedByUuid(),
                record.shelvedByName(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    public enum MarkResult {
        MARKED,
        ALREADY_MARKED,
        INVALID_BLOCK
    }

    public enum UnmarkResult {
        UNMARKED,
        NOT_MARKED,
        NOT_EMPTY
    }

    public enum StoreResult {
        STORED,
        NO_SHELF,
        INVALID_SLOT,
        INVALID_ITEM,
        SLOT_OCCUPIED,
        AUTHOR_MISMATCH
    }

    public enum RemoveResult {
        REMOVED,
        NO_BOOK,
        NOT_ALLOWED,
        NO_SPACE
    }
}
