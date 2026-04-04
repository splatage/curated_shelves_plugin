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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
        for (LibraryShelf shelf : this.repository.loadShelves()) {
            this.shelvesByLocation.put(shelf.location(), shelf);
            this.shelvesById.put(shelf.shelfId(), shelf);
            this.booksByShelf.putIfAbsent(shelf.shelfId(), new ConcurrentHashMap<>());
        }
        for (LibraryBook book : this.repository.loadBooks()) {
            this.booksByShelf.computeIfAbsent(book.shelfId(), ignored -> new ConcurrentHashMap<>())
                    .put(book.slotIndex(), book);
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
        return Optional.ofNullable(this.shelvesByLocation.get(locationKey));
    }

    public Optional<LibraryShelf> shelfById(final UUID shelfId) {
        return Optional.ofNullable(this.shelvesById.get(shelfId));
    }

    public LibraryShelfSnapshot snapshot(final UUID shelfId) {
        final LibraryShelf shelf = Objects.requireNonNull(this.shelvesById.get(shelfId), "Unknown shelf: " + shelfId);
        final Map<Integer, LibraryBook> books = this.booksByShelf.getOrDefault(shelfId, Collections.emptyMap());
        return new LibraryShelfSnapshot(shelf, books);
    }

    public Collection<LibraryShelf> allShelves() {
        return Collections.unmodifiableCollection(this.shelvesById.values());
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
        for (Map<Integer, LibraryBook> books : this.booksByShelf.values()) {
            for (LibraryBook book : books.values()) {
                if (book.bookId().equals(bookId)) {
                    return Optional.of(book);
                }
            }
        }
        return Optional.empty();
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
        return book.author() != null && !book.author().isBlank() && player.getName().equalsIgnoreCase(book.author());
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

    public LibraryShelf newShelf(final LocationKey locationKey, final int rows) {
        final long now = Instant.now().toEpochMilli();
        return new LibraryShelf(UUID.randomUUID(), locationKey, rows, now, now);
    }

    public LibraryBook newBook(final UUID shelfId, final int slotIndex, final ItemStack itemStack, final Player player) {
        final BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
        final long now = Instant.now().toEpochMilli();
        return new LibraryBook(
                UUID.randomUUID(),
                shelfId,
                slotIndex,
                BookCodec.serializeItem(itemStack),
                normalizeTitle(bookMeta.getTitle()),
                normalizeText(bookMeta.getAuthor()),
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
                this.repository.upsertShelf(shelf);
                this.shelvesByLocation.put(shelf.location(), shelf);
                this.shelvesById.put(shelf.shelfId(), shelf);
                this.booksByShelf.putIfAbsent(shelf.shelfId(), new ConcurrentHashMap<>());
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
                this.repository.deleteBooksForShelf(shelfId);
                this.repository.deleteShelf(shelfId);
                this.shelvesById.remove(shelfId);
                this.shelvesByLocation.remove(shelf.location());
                this.booksByShelf.remove(shelfId);
                onSuccess.run();
            } catch (final Throwable throwable) {
                onFailure.accept(throwable);
            }
        });
    }

    public void storeBook(final LibraryBook book, final Runnable onSuccess, final Consumer<Throwable> onFailure) {
        this.schedulerFacade.runAsync(() -> {
            try {
                this.repository.upsertBook(book);
                this.booksByShelf.computeIfAbsent(book.shelfId(), ignored -> new ConcurrentHashMap<>())
                        .put(book.slotIndex(), book);
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
                final Map<Integer, LibraryBook> books = this.booksByShelf.get(book.shelfId());
                if (books != null) {
                    books.remove(book.slotIndex());
                }
                onSuccess.run();
            } catch (final Throwable throwable) {
                onFailure.accept(throwable);
            }
        });
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
