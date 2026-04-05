package com.splatage.curatedshelves.service;

import com.splatage.curatedshelves.config.PluginConfig;
import com.splatage.curatedshelves.data.LibraryRepository;
import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.domain.LocationKey;
import com.splatage.curatedshelves.platform.SchedulerFacade;
import com.splatage.curatedshelves.util.BookCodec;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LibraryServiceTest {
    @Test
    void initializeDeletesOrphanBooksAndIndexesOnlyValidBooks() throws SQLException {
        final FakeRepository repository = new FakeRepository();
        final LibraryShelf shelf = new LibraryShelf(UUID.randomUUID(), new LocationKey(UUID.randomUUID(), 1, 2, 3), 1, UUID.randomUUID(), "Creator", 1L, 1L);
        repository.shelves.add(shelf);
        final LibraryBook validBook = new LibraryBook(UUID.randomUUID(), shelf.shelfId(), 0, "a", "Valid", "Author", UUID.randomUUID(), UUID.randomUUID(), "Shelver", 1L, 1L);
        final LibraryBook orphanBook = new LibraryBook(UUID.randomUUID(), UUID.randomUUID(), 1, "b", "Orphan", "Author", UUID.randomUUID(), UUID.randomUUID(), "Shelver", 1L, 1L);
        repository.books.add(validBook);
        repository.books.add(orphanBook);

        final LibraryService service = new LibraryService(plugin(), new ImmediateSchedulerFacade(), repository);
        service.initialize();

        assertTrue(repository.deleteOrphanBooksCalled, "Expected orphan cleanup to run during initialization");
        assertTrue(service.bookById(validBook.bookId()).isPresent(), "Valid book should be indexed");
        assertTrue(service.bookById(orphanBook.bookId()).isEmpty(), "Orphan book should not be indexed");
    }

    @Test
    void initializeRemovesDuplicateShelvesAtSameLocationKeepingNewest() throws SQLException {
        final FakeRepository repository = new FakeRepository();
        final LocationKey location = new LocationKey(UUID.randomUUID(), 4, 5, 6);
        final LibraryShelf olderShelf = new LibraryShelf(UUID.randomUUID(), location, 1, UUID.randomUUID(), "Older", 10L, 20L);
        final LibraryShelf newerShelf = new LibraryShelf(UUID.randomUUID(), location, 2, UUID.randomUUID(), "Newer", 30L, 40L);
        repository.shelves.add(olderShelf);
        repository.shelves.add(newerShelf);
        repository.books.add(new LibraryBook(UUID.randomUUID(), olderShelf.shelfId(), 0, "older", "Old", "Author", null, UUID.randomUUID(), "Shelver", 1L, 1L));
        repository.books.add(new LibraryBook(UUID.randomUUID(), newerShelf.shelfId(), 0, "newer", "New", "Author", null, UUID.randomUUID(), "Shelver", 1L, 1L));

        final LibraryService service = new LibraryService(plugin(), new ImmediateSchedulerFacade(), repository);
        service.initialize();

        assertEquals(Set.of(olderShelf.shelfId()), repository.deletedShelfIds, "Older duplicate shelf should be removed during initialization");
        assertTrue(service.shelfById(newerShelf.shelfId()).isPresent(), "Newest shelf should remain loaded");
        assertTrue(service.shelfById(olderShelf.shelfId()).isEmpty(), "Older shelf should be removed from runtime state");
        assertEquals("New", service.bookAt(newerShelf.shelfId(), 0).orElseThrow().title());
    }

    @Test
    void newShelfCapturesCreatorMetadataFromPlayer() {
        final LibraryService service = new LibraryService(plugin(), new ImmediateSchedulerFacade(), new FakeRepository());
        final Player creator = mock(Player.class);
        final UUID creatorUuid = UUID.randomUUID();
        when(creator.getUniqueId()).thenReturn(creatorUuid);
        when(creator.getName()).thenReturn("Creator");

        final LibraryShelf shelf = service.newShelf(new LocationKey(UUID.randomUUID(), 7, 8, 9), 2, creator);

        assertEquals(creatorUuid, shelf.createdByUuid());
        assertEquals("Creator", shelf.createdByName());
    }

    @Test
    void createShelfReplacesExistingRuntimeShelfAtSameLocation() {
        final FakeRepository repository = new FakeRepository();
        final LibraryService service = new LibraryService(plugin(), new ImmediateSchedulerFacade(), repository);
        final LocationKey location = new LocationKey(UUID.randomUUID(), 10, 64, 10);
        final LibraryShelf existingShelf = new LibraryShelf(UUID.randomUUID(), location, 1, UUID.randomUUID(), "Existing", 1L, 1L);
        final LibraryBook existingBook = new LibraryBook(UUID.randomUUID(), existingShelf.shelfId(), 0, "a", "First", "Author", null, UUID.randomUUID(), "Shelver", 1L, 1L);
        repository.shelves.add(existingShelf);
        repository.books.add(existingBook);
        assertDoesNotThrow(service::initialize);
        assertTrue(service.bookById(existingBook.bookId()).isPresent());

        final LibraryShelf replacementShelf = new LibraryShelf(UUID.randomUUID(), location, 2, UUID.randomUUID(), "Replacement", 2L, 2L);
        service.createShelf(replacementShelf, () -> { }, throwable -> fail(throwable));

        assertTrue(service.shelfById(replacementShelf.shelfId()).isPresent(), "Replacement shelf should be loaded");
        assertTrue(service.shelfById(existingShelf.shelfId()).isEmpty(), "Replaced shelf should be dropped from runtime state");
        assertTrue(service.bookById(existingBook.bookId()).isEmpty(), "Books from the replaced shelf should be removed from the id index");
    }

    @Test
    void bookIdIndexTracksStoreRemoveAndDeleteShelf() {
        final FakeRepository repository = new FakeRepository();
        final LibraryService service = new LibraryService(plugin(), new ImmediateSchedulerFacade(), repository);
        final LibraryShelf shelf = new LibraryShelf(UUID.randomUUID(), new LocationKey(UUID.randomUUID(), 10, 64, 10), 1, UUID.randomUUID(), "Creator", 1L, 1L);

        service.createShelf(shelf, () -> { }, throwable -> fail(throwable));

        final LibraryBook firstBook = new LibraryBook(UUID.randomUUID(), shelf.shelfId(), 0, "a", "First", "Author", UUID.randomUUID(), UUID.randomUUID(), "Shelver", 1L, 1L);
        final LibraryBook secondBook = new LibraryBook(UUID.randomUUID(), shelf.shelfId(), 1, "b", "Second", "Author", UUID.randomUUID(), UUID.randomUUID(), "Shelver", 1L, 1L);

        service.storeBook(firstBook, () -> { }, throwable -> fail(throwable));
        service.storeBook(secondBook, () -> { }, throwable -> fail(throwable));
        assertTrue(service.bookById(firstBook.bookId()).isPresent());
        assertTrue(service.bookById(secondBook.bookId()).isPresent());

        service.removeBook(firstBook, () -> { }, throwable -> fail(throwable));
        assertTrue(service.bookById(firstBook.bookId()).isEmpty(), "Removed book should leave the id index");
        assertTrue(service.bookById(secondBook.bookId()).isPresent(), "Other books should remain indexed");

        service.deleteShelf(shelf.shelfId(), () -> { }, throwable -> fail(throwable));
        assertTrue(service.bookById(secondBook.bookId()).isEmpty(), "Deleting a shelf should clear indexed books for that shelf");
    }

    @Test
    void authorRemovalUsesStableAuthorUuidInsteadOfNameOnly() {
        final LibraryService service = new LibraryService(plugin(), new ImmediateSchedulerFacade(), new FakeRepository());
        final UUID authorUuid = UUID.randomUUID();
        final LibraryBook book = new LibraryBook(UUID.randomUUID(), UUID.randomUUID(), 0, "serialized", "Title", "Alice", authorUuid, UUID.randomUUID(), "Shelver", 1L, 1L);
        final PluginConfig config = defaultPluginConfig(true);

        final Player sameNameDifferentUuid = mock(Player.class);
        when(sameNameDifferentUuid.hasPermission("curatedshelves.librarian.remove.any")).thenReturn(false);
        when(sameNameDifferentUuid.getUniqueId()).thenReturn(UUID.randomUUID());
        when(sameNameDifferentUuid.getName()).thenReturn("Alice");

        final Player matchingUuid = mock(Player.class);
        when(matchingUuid.hasPermission("curatedshelves.librarian.remove.any")).thenReturn(false);
        when(matchingUuid.getUniqueId()).thenReturn(authorUuid);
        when(matchingUuid.getName()).thenReturn("Alice");

        assertFalse(service.canRemoveBook(sameNameDifferentUuid, book, config), "Matching the author name alone should not grant removal");
        assertTrue(service.canRemoveBook(matchingUuid, book, config), "Matching the stored author UUID should grant removal");
    }

    @Test
    void newBookCapturesOnlineAuthorUuidWhenDepositorIsNotAuthor() {
        final Server server = mock(Server.class);
        final Player author = mock(Player.class);
        final UUID authorUuid = UUID.randomUUID();
        when(author.getUniqueId()).thenReturn(authorUuid);
        when(server.getPlayerExact("Alice")).thenReturn(author);

        final Plugin plugin = plugin(server);
        final LibraryService service = new LibraryService(plugin, new ImmediateSchedulerFacade(), new FakeRepository());

        final Player depositor = mock(Player.class);
        when(depositor.getUniqueId()).thenReturn(UUID.randomUUID());
        when(depositor.getName()).thenReturn("Bob");

        final ItemStack itemStack = mock(ItemStack.class);
        final BookMeta bookMeta = mock(BookMeta.class);
        when(itemStack.getItemMeta()).thenReturn(bookMeta);
        when(bookMeta.getAuthor()).thenReturn("Alice");
        when(bookMeta.getTitle()).thenReturn("Story");

        try (MockedStatic<BookCodec> codec = Mockito.mockStatic(BookCodec.class)) {
            codec.when(() -> BookCodec.serializeItem(itemStack)).thenReturn("encoded");
            final LibraryBook book = service.newBook(UUID.randomUUID(), 0, itemStack, depositor);
            assertEquals(authorUuid, book.authorUuid(), "Online authored books should capture the author's UUID even when another player shelves them");
        }
    }


    private PluginConfig defaultPluginConfig(final boolean authorCanRemoveOwnBook) {
        return new PluginConfig(
                1,
                false,
                authorCanRemoveOwnBook,
                new PluginConfig.StorageConfig(
                        PluginConfig.StorageType.SQLITE,
                        "",
                        new PluginConfig.SqliteConfig("library.db"),
                        new PluginConfig.MysqlConfig("localhost", 3306, "curated_shelves", "root", "", 10, 10000L)
                )
        );
    }

    private Plugin plugin() {
        return plugin(mock(Server.class));
    }

    private Plugin plugin(final Server server) {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LibraryServiceTest"));
        when(plugin.getServer()).thenReturn(server);
        return plugin;
    }

    private static final class ImmediateSchedulerFacade implements SchedulerFacade {
        @Override
        public void runGlobal(final Runnable task) {
            task.run();
        }

        @Override
        public void runAtLocation(final Location location, final Runnable task) {
            task.run();
        }

        @Override
        public void runForPlayer(final Player player, final Runnable task) {
            task.run();
        }

        @Override
        public void runForPlayer(final Player player, final Runnable task, final Runnable retiredTask) {
            task.run();
        }

        @Override
        public void runAsync(final Runnable task) {
            task.run();
        }
    }

    private static final class FakeRepository implements LibraryRepository {
        private final List<LibraryShelf> shelves = new ArrayList<>();
        private final List<LibraryBook> books = new ArrayList<>();
        private final Set<UUID> deletedShelfIds = new HashSet<>();
        private boolean deleteOrphanBooksCalled;

        @Override
        public void initialize() {
        }

        @Override
        public List<LibraryShelf> loadShelves() {
            return List.copyOf(this.shelves);
        }

        @Override
        public List<LibraryBook> loadBooks() {
            return List.copyOf(this.books);
        }

        @Override
        public List<UUID> replaceShelfAtLocation(final LibraryShelf shelf) {
            final List<UUID> replaced = new ArrayList<>();
            for (LibraryShelf existing : List.copyOf(this.shelves)) {
                if (!existing.location().equals(shelf.location()) || existing.shelfId().equals(shelf.shelfId())) {
                    continue;
                }
                replaced.add(existing.shelfId());
                deleteShelfCascade(existing.shelfId());
            }
            this.shelves.removeIf(existing -> existing.shelfId().equals(shelf.shelfId()));
            this.shelves.add(shelf);
            return replaced;
        }

        @Override
        public void deleteShelfCascade(final UUID shelfId) {
            this.deletedShelfIds.add(shelfId);
            removeBooksForShelf(shelfId);
            this.shelves.removeIf(shelf -> shelf.shelfId().equals(shelfId));
        }

        @Override
        public void upsertBook(final LibraryBook book) {
            this.books.removeIf(existing -> existing.bookId().equals(book.bookId()));
            this.books.add(book);
        }

        @Override
        public void deleteBook(final UUID bookId) {
            this.books.removeIf(book -> book.bookId().equals(bookId));
        }

        private void removeBooksForShelf(final UUID shelfId) {
            this.books.removeIf(book -> book.shelfId().equals(shelfId));
        }

        @Override
        public void deleteOrphanBooks() {
            this.deleteOrphanBooksCalled = true;
            final java.util.Set<UUID> validShelfIds = ConcurrentHashMap.newKeySet();
            for (LibraryShelf shelf : this.shelves) {
                validShelfIds.add(shelf.shelfId());
            }
            this.books.removeIf(book -> !validShelfIds.contains(book.shelfId()));
        }

        @Override
        public void close() {
        }
    }
}
