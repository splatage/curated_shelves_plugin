package com.splatage.curatedshelves.service;

import com.splatage.curatedshelves.data.LibraryRepository;
import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.domain.LocationKey;
import com.splatage.curatedshelves.platform.SchedulerFacade;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryServiceStateTest {
    @Test
    void createdShelfIsHiddenUntilActivated() {
        final FakeRepository repository = new FakeRepository();
        final LibraryService service = new LibraryService(plugin(), new ImmediateScheduler(), repository);
        final LibraryShelf shelf = new LibraryShelf(
                UUID.randomUUID(),
                new LocationKey(UUID.randomUUID(), 20, 64, 20),
                1,
                UUID.randomUUID(),
                "Creator",
                2L,
                2L
        );

        service.createShelf(shelf, () -> { }, throwable -> { throw new AssertionError(throwable); });

        assertTrue(service.shelfById(shelf.shelfId()).isEmpty());
        assertTrue(service.snapshotIfPresent(shelf.shelfId()).isEmpty());
        assertTrue(service.allShelfSnapshotsSorted().isEmpty());

        assertTrue(service.activateCreatedShelf(shelf.shelfId()));
        assertTrue(service.shelfById(shelf.shelfId()).isPresent());
        assertTrue(service.snapshotIfPresent(shelf.shelfId()).isPresent());
        assertEquals(1, service.allShelfSnapshotsSorted().size());
    }

    @Test
    void pendingShelfRemovalHidesShelfFromSnapshotsAndBrowserList() {
        final FakeRepository repository = new FakeRepository();
        final LibraryService service = new LibraryService(plugin(), new ImmediateScheduler(), repository);
        final LibraryShelf shelf = new LibraryShelf(
                UUID.randomUUID(),
                new LocationKey(UUID.randomUUID(), 10, 64, 10),
                1,
                UUID.randomUUID(),
                "Tester",
                1L,
                1L
        );

        service.createShelf(shelf, () -> { }, throwable -> { throw new AssertionError(throwable); });
        assertTrue(service.activateCreatedShelf(shelf.shelfId()));

        assertTrue(service.shelfById(shelf.shelfId()).isPresent());
        assertTrue(service.snapshotIfPresent(shelf.shelfId()).isPresent());
        assertEquals(1, service.allShelfSnapshotsSorted().size());

        assertTrue(service.beginShelfRemoval(shelf.shelfId()));
        assertFalse(service.shelfById(shelf.shelfId()).isPresent());
        assertFalse(service.snapshotIfPresent(shelf.shelfId()).isPresent());
        assertTrue(service.allShelfSnapshotsSorted().isEmpty());
    }

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "CuratedShelvesTest";
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(final Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0F;
        }
        if (returnType == double.class) {
            return 0.0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class ImmediateScheduler implements SchedulerFacade {
        @Override
        public void runGlobal(final Runnable task) {
            task.run();
        }

        @Override
        public void runAtLocation(final org.bukkit.Location location, final Runnable task) {
            task.run();
        }

        @Override
        public void runForPlayer(final org.bukkit.entity.Player player, final Runnable task) {
            task.run();
        }

        @Override
        public void runForPlayer(final org.bukkit.entity.Player player, final Runnable task, final Runnable retiredTask) {
            task.run();
        }

        @Override
        public void runAsync(final Runnable task) {
            task.run();
        }
    }

    private static final class FakeRepository implements LibraryRepository {
        private final List<LibraryShelf> shelves = new ArrayList<>();

        @Override
        public void initialize() {
        }

        @Override
        public List<LibraryShelf> loadShelves() {
            return List.copyOf(this.shelves);
        }

        @Override
        public List<LibraryBook> loadBooks() {
            return List.of();
        }

        @Override
        public List<UUID> replaceShelfAtLocation(final LibraryShelf shelf) {
            this.shelves.removeIf(existing -> existing.location().equals(shelf.location()) && !existing.shelfId().equals(shelf.shelfId()));
            this.shelves.removeIf(existing -> existing.shelfId().equals(shelf.shelfId()));
            this.shelves.add(shelf);
            return List.of();
        }

        @Override
        public void deleteShelfCascade(final UUID shelfId) {
            this.shelves.removeIf(existing -> existing.shelfId().equals(shelfId));
        }

        @Override
        public void upsertBook(final LibraryBook book) {
        }

        @Override
        public void deleteBook(final UUID bookId) {
        }

        @Override
        public void deleteOrphanBooks() {
        }

        @Override
        public void close() throws SQLException {
        }
    }
}
