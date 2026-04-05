package com.splatage.curatedshelves.service;

import com.splatage.curatedshelves.config.PluginConfig;
import com.splatage.curatedshelves.data.LibraryRepository;
import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.domain.LibraryShelfSnapshot;
import com.splatage.curatedshelves.domain.LocationKey;
import com.splatage.curatedshelves.platform.SchedulerFacade;
import com.splatage.curatedshelves.util.BookCodec;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class LibraryService {
    private final Plugin plugin;
    private final SchedulerFacade schedulerFacade;
    private final LibraryRepository repository;
    private final Map<LocationKey, LibraryShelf> shelvesByLocation = new ConcurrentHashMap<>();
    private final Map<UUID, LibraryShelf> shelvesById = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, LibraryBook>> booksByShelf = new ConcurrentHashMap<>();
    private final Map<UUID, LibraryBook> booksById = new ConcurrentHashMap<>();
    private final Set<UUID> shelvesPendingRemoval = ConcurrentHashMap.newKeySet();

    public LibraryService(
            final Plugin plugin,
            final SchedulerFacade schedulerFacade,
            final LibraryRepository repository
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.schedulerFacade = Objects.requireNonNull(schedulerFacade, "schedulerFacade");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void initialize() throws SQLException {
        this.repository.initialize();

        final Map<LocationKey, LibraryShelf> selectedShelves = new LinkedHashMap<>();
        for (LibraryShelf shelf : this.repository.loadShelves()) {
            final LibraryShelf existing = selectedShelves.get(shelf.location());
            if (existing == null) {
                selectedShelves.put(shelf.location(), shelf);
                continue;
            }
            final LibraryShelf keep = existing.updatedAt() >= shelf.updatedAt() ? existing : shelf;
            final LibraryShelf drop = keep == existing ? shelf : existing;
            selectedShelves.put(keep.location(), keep);
            this.repository.deleteShelfCascade(drop.shelfId());
            this.plugin.getLogger().warning(
                    "Removed duplicate persisted Library Shelf " + drop.shelfId() + " at " + drop.location()
                            + " in favor of " + keep.shelfId()
            );
        }

        for (LibraryShelf shelf : selectedShelves.values()) {
            putShelfRuntime(shelf);
        }

        this.repository.deleteOrphanBooks();
        for (LibraryBook book : this.repository.loadBooks()) {
            if (!this.shelvesById.containsKey(book.shelfId())) {
                this.plugin.getLogger().warning("Ignoring orphaned library book " + book.bookId() + " for missing shelf " + book.shelfId());
                continue;
            }
            putBookRuntime(book);
        }
    }

    public void shutdown() {
        try {
            this.repository.close();
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to close library repository", exception);
        }
    }

    public Optional<LibraryShelf> shelfAt(final LocationKey locationKey) {
        final LibraryShelf shelf = this.shelvesByLocation.get(locationKey);
        if (shelf == null || this.shelvesPendingRemoval.contains(shelf.shelfId())) {
            return Optional.empty();
        }
        return Optional.of(shelf);
    }

    public Optional<LibraryShelf> shelfById(final UUID shelfId) {
        final LibraryShelf shelf = this.shelvesById.get(shelfId);
        if (shelf == null || this.shelvesPendingRemoval.contains(shelfId)) {
            return Optional.empty();
        }
        return Optional.of(shelf);
    }

    public Optional<LibraryShelfSnapshot> snapshotIfPresent(final UUID shelfId) {
        if (this.shelvesPendingRemoval.contains(shelfId)) {
            return Optional.empty();
        }
        final LibraryShelf shelf = this.shelvesById.get(shelfId);
        if (shelf == null) {
            return Optional.empty();
        }
        final Map<Integer, LibraryBook> books = this.booksByShelf.getOrDefault(shelfId, Collections.emptyMap());
        return Optional.of(new LibraryShelfSnapshot(shelf, books));
    }

    public LibraryShelfSnapshot snapshot(final UUID shelfId) {
        return snapshotIfPresent(shelfId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown shelf: " + shelfId));
    }

    public Collection<LibraryShelf> allShelves() {
        return Collections.unmodifiableList(this.shelvesById.values().stream()
                .filter(shelf -> !this.shelvesPendingRemoval.contains(shelf.shelfId()))
                .toList());
    }

    public List<LibraryShelfSnapshot> allShelfSnapshotsSorted() {
        return this.shelvesById.values().stream()
                .filter(shelf -> !this.shelvesPendingRemoval.contains(shelf.shelfId()))
                .sorted(Comparator
                        .comparing((LibraryShelf shelf) -> shelf.location().worldUuid())
                        .thenComparingInt(shelf -> shelf.location().x())
                        .thenComparingInt(shelf -> shelf.location().y())
                        .thenComparingInt(shelf -> shelf.location().z()))
                .map(shelf -> new LibraryShelfSnapshot(
                        shelf,
                        this.booksByShelf.getOrDefault(shelf.shelfId(), Collections.emptyMap())
                ))
                .toList();
    }

    public boolean beginShelfRemoval(final UUID shelfId) {
        if (!this.shelvesById.containsKey(shelfId)) {
            return false;
        }
        return this.shelvesPendingRemoval.add(shelfId);
    }

    public boolean isShelfPendingRemoval(final UUID shelfId) {
        return this.shelvesPendingRemoval.contains(shelfId);
    }

    public int firstEmptySlot(final UUID shelfId) {
        final LibraryShelf shelf = Objects.requireNonNull(this.shelvesById.get(shelfId), "Unknown shelf: " + shelfId);
        final Map<Integer, LibraryBook> books = this.booksByShelf.getOrDefault(shelfId, Collections.emptyMap());
        final int maxSlots = shelf.rows() * 9;
        for (int index = 0; index < maxSlots; index++) {
            if (!books.containsKey(index)) {
                return index;
            }
        }
        return -1;
    }

    public Optional<LibraryBook> bookAt(final UUID shelfId, final int slotIndex) {
        return Optional.ofNullable(this.booksByShelf.getOrDefault(shelfId, Collections.emptyMap()).get(slotIndex));
    }

    public Optional<LibraryBook> bookById(final UUID bookId) {
        return Optional.ofNullable(this.booksById.get(bookId));
    }

    public boolean canRemoveBook(final Player player, final LibraryBook book, final PluginConfig config) {
        if (player.hasPermission("curatedshelves.librarian.remove.any")) {
            return true;
        }
        if (player.getUniqueId().equals(book.shelvedByUuid())) {
            return true;
        }
        if (!config.authorCanRemoveOwnBook()) {
            return false;
        }
        return book.authorUuid() != null && player.getUniqueId().equals(book.authorUuid());
    }

    public boolean canDepositBook(final Player player, final ItemStack itemStack, final PluginConfig config) {
        if (!(itemStack.getItemMeta() instanceof BookMeta bookMeta)) {
            return false;
        }
        if (!config.authorMustDeposit()) {
            return true;
        }
        final String author = bookMeta.getAuthor();
        return author != null && !author.isBlank() && player.getName().equalsIgnoreCase(author);
    }

    public LibraryShelf newShelf(final LocationKey locationKey, final int rows, final Player player) {
        final long now = Instant.now().toEpochMilli();
        return new LibraryShelf(
                UUID.randomUUID(),
                locationKey,
                rows,
                player.getUniqueId(),
                player.getName(),
                now,
                now
        );
    }

    public LibraryBook newBook(final UUID shelfId, final int slotIndex, final ItemStack itemStack, final Player player) {
        final BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
        final long now = Instant.now().toEpochMilli();
        final String normalizedAuthor = normalizeText(bookMeta.getAuthor());
        final UUID authorUuid = resolveAuthorUuid(normalizedAuthor, player);
        return new LibraryBook(
                UUID.randomUUID(),
                shelfId,
                slotIndex,
                BookCodec.serializeItem(itemStack),
                normalizeTitle(bookMeta.getTitle()),
                normalizedAuthor,
                authorUuid,
                player.getUniqueId(),
                player.getName(),
                now,
                now
        );
    }

    public Map<Integer, LibraryBook> sortedBooks(final UUID shelfId) {
        final Map<Integer, LibraryBook> books = this.booksByShelf.getOrDefault(shelfId, Collections.emptyMap());
        return books.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
    }

    public void createShelf(final LibraryShelf shelf, final Runnable onSuccess, final Consumer<Throwable> onFailure) {
        this.schedulerFacade.runAsync(() -> {
            try {
                final List<UUID> replacedShelfIds = this.repository.replaceShelfAtLocation(shelf);
                for (UUID replacedShelfId : replacedShelfIds) {
                    removeShelfRuntime(replacedShelfId);
                }
                putShelfRuntime(shelf);
                onSuccess.run();
            } catch (final Throwable throwable) {
                onFailure.accept(throwable);
            }
        });
    }

    public void deleteShelf(final UUID shelfId, final Runnable onSuccess, final Consumer<Throwable> onFailure) {
        final LibraryShelf shelf = this.shelvesById.get(shelfId);
        if (shelf == null) {
            onFailure.accept(new IllegalStateException("Unknown shelf: " + shelfId));
            return;
        }
        this.schedulerFacade.runAsync(() -> {
            try {
                this.repository.deleteShelfCascade(shelfId);
                removeShelfRuntime(shelfId);
                onSuccess.run();
            } catch (final Throwable throwable) {
                onFailure.accept(throwable);
            } finally {
                this.shelvesPendingRemoval.remove(shelfId);
            }
        });
    }

    public void discardShelfRuntime(final UUID shelfId) {
        removeShelfRuntime(shelfId);
    }

    public void storeBook(final LibraryBook book, final Runnable onSuccess, final Consumer<Throwable> onFailure) {
        this.schedulerFacade.runAsync(() -> {
            try {
                this.repository.upsertBook(book);
                putBookRuntime(book);
                onSuccess.run();
            } catch (final Throwable throwable) {
                onFailure.accept(throwable);
            }
        });
    }

    public void removeBook(final LibraryBook book, final Runnable onSuccess, final Consumer<Throwable> onFailure) {
        this.schedulerFacade.runAsync(() -> {
            try {
                this.repository.deleteBook(book.bookId());
                removeBookRuntime(book);
                onSuccess.run();
            } catch (final Throwable throwable) {
                onFailure.accept(throwable);
            }
        });
    }

    private UUID resolveAuthorUuid(final String normalizedAuthor, final Player depositingPlayer) {
        if (normalizedAuthor == null) {
            return null;
        }
        if (depositingPlayer.getName().equalsIgnoreCase(normalizedAuthor)) {
            return depositingPlayer.getUniqueId();
        }
        final Player onlineAuthor = this.plugin.getServer().getPlayerExact(normalizedAuthor);
        if (onlineAuthor != null) {
            return onlineAuthor.getUniqueId();
        }
        return null;
    }

    private void putShelfRuntime(final LibraryShelf shelf) {
        final LibraryShelf replacedAtLocation = this.shelvesByLocation.put(shelf.location(), shelf);
        if (replacedAtLocation != null && !replacedAtLocation.shelfId().equals(shelf.shelfId())) {
            removeShelfRuntime(replacedAtLocation.shelfId());
        }
        this.shelvesById.put(shelf.shelfId(), shelf);
        this.booksByShelf.putIfAbsent(shelf.shelfId(), new ConcurrentHashMap<>());
    }

    private void putBookRuntime(final LibraryBook book) {
        this.booksByShelf.computeIfAbsent(book.shelfId(), ignored -> new ConcurrentHashMap<>())
                .put(book.slotIndex(), book);
        this.booksById.put(book.bookId(), book);
    }

    private void removeBookRuntime(final LibraryBook book) {
        final Map<Integer, LibraryBook> books = this.booksByShelf.get(book.shelfId());
        if (books != null) {
            books.remove(book.slotIndex());
        }
        this.booksById.remove(book.bookId());
    }

    private void removeShelfRuntime(final UUID shelfId) {
        this.shelvesPendingRemoval.remove(shelfId);
        final LibraryShelf removedShelf = this.shelvesById.remove(shelfId);
        if (removedShelf != null) {
            this.shelvesByLocation.remove(removedShelf.location(), removedShelf);
        }
        final Map<Integer, LibraryBook> removedBooks = this.booksByShelf.remove(shelfId);
        if (removedBooks != null) {
            for (LibraryBook removedBook : removedBooks.values()) {
                this.booksById.remove(removedBook.bookId());
            }
        }
    }

    private String normalizeTitle(final String rawTitle) {
        final String normalized = normalizeText(rawTitle);
        return normalized != null ? normalized : "Untitled Book";
    }

    private String normalizeText(final String raw) {
        if (raw == null) {
            return null;
        }
        final String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
