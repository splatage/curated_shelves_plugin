package com.splatage.curatedshelves.data;

import com.splatage.curatedshelves.platform.SchedulerFacade;
import com.splatage.curatedshelves.util.ItemSerialization;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShelfStore {
    private final Plugin plugin;
    private final SchedulerFacade schedulerFacade;
    private final File file;
    private final Map<String, LibraryShelf> shelvesById;
    private final Map<ShelfKey, String> shelfIdsByLocation;
    private final Object writeLock;

    public ShelfStore(final Plugin plugin, final SchedulerFacade schedulerFacade) {
        this.plugin = plugin;
        this.schedulerFacade = schedulerFacade;
        this.file = new File(plugin.getDataFolder(), "shelves.yml");
        this.shelvesById = new ConcurrentHashMap<>();
        this.shelfIdsByLocation = new ConcurrentHashMap<>();
        this.writeLock = new Object();
    }

    public synchronized void load() {
        this.shelvesById.clear();
        this.shelfIdsByLocation.clear();
        if (!this.file.exists()) {
            return;
        }

        final YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(this.file);
        final Map<String, Object> shelvesSection = yamlConfiguration.getConfigurationSection("shelves") == null
                ? Map.of()
                : yamlConfiguration.getConfigurationSection("shelves").getValues(false);

        for (final String shelfId : shelvesSection.keySet()) {
            final String base = "shelves." + shelfId;
            final String worldUid = yamlConfiguration.getString(base + ".world-uid");
            if (worldUid == null) {
                continue;
            }
            final ShelfKey key = new ShelfKey(
                    worldUid,
                    yamlConfiguration.getInt(base + ".x"),
                    yamlConfiguration.getInt(base + ".y"),
                    yamlConfiguration.getInt(base + ".z")
            );
            final LibraryShelf shelf = new LibraryShelf(shelfId, key);
            final String booksBase = base + ".books";
            if (yamlConfiguration.getConfigurationSection(booksBase) != null) {
                for (final String slotKey : yamlConfiguration.getConfigurationSection(booksBase).getKeys(false)) {
                    final int slot = Integer.parseInt(slotKey);
                    final String slotBase = booksBase + "." + slotKey;
                    final String serialized = yamlConfiguration.getString(slotBase + ".item");
                    if (serialized == null) {
                        continue;
                    }
                    final ItemStack itemStack = ItemSerialization.deserialize(serialized);
                    shelf.putBook(new LibraryBook(
                            slot,
                            itemStack,
                            yamlConfiguration.getString(slotBase + ".title", "Untitled Book"),
                            yamlConfiguration.getString(slotBase + ".author", "Unknown"),
                            yamlConfiguration.getString(slotBase + ".shelved-by-uuid", ""),
                            yamlConfiguration.getString(slotBase + ".shelved-by-name", "Unknown")
                    ));
                }
            }
            this.shelvesById.put(shelfId, shelf);
            this.shelfIdsByLocation.put(key, shelfId);
        }
    }

    public synchronized void saveBlocking() {
        final YamlConfiguration yamlConfiguration = snapshotToYaml();
        this.file.getParentFile().mkdirs();
        try {
            yamlConfiguration.save(this.file);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to save shelves.yml", exception);
        }
    }

    public void scheduleSave() {
        final YamlConfiguration snapshot;
        synchronized (this) {
            snapshot = snapshotToYaml();
        }
        this.schedulerFacade.executeAsync(() -> {
            synchronized (this.writeLock) {
                this.file.getParentFile().mkdirs();
                try {
                    snapshot.save(this.file);
                } catch (final IOException exception) {
                    this.plugin.getLogger().warning("Failed to save shelves.yml: " + exception.getMessage());
                }
            }
        });
    }

    private YamlConfiguration snapshotToYaml() {
        final YamlConfiguration yamlConfiguration = new YamlConfiguration();
        for (final LibraryShelf shelf : this.shelvesById.values()) {
            final String base = "shelves." + shelf.shelfId();
            yamlConfiguration.set(base + ".world-uid", shelf.key().worldUid());
            yamlConfiguration.set(base + ".x", shelf.key().x());
            yamlConfiguration.set(base + ".y", shelf.key().y());
            yamlConfiguration.set(base + ".z", shelf.key().z());
            for (final LibraryBook book : shelf.books()) {
                final String slotBase = base + ".books." + book.slotIndex();
                yamlConfiguration.set(slotBase + ".item", ItemSerialization.serialize(book.item()));
                yamlConfiguration.set(slotBase + ".title", book.title());
                yamlConfiguration.set(slotBase + ".author", book.author());
                yamlConfiguration.set(slotBase + ".shelved-by-uuid", book.shelvedByUuid());
                yamlConfiguration.set(slotBase + ".shelved-by-name", book.shelvedByName());
            }
        }
        return yamlConfiguration;
    }

    public synchronized Optional<LibraryShelf> findByKey(final ShelfKey shelfKey) {
        final String shelfId = this.shelfIdsByLocation.get(shelfKey);
        return shelfId == null ? Optional.empty() : Optional.ofNullable(this.shelvesById.get(shelfId));
    }

    public synchronized Optional<LibraryShelf> findByShelfId(final String shelfId) {
        return Optional.ofNullable(this.shelvesById.get(shelfId));
    }

    public synchronized LibraryShelf createShelf(final ShelfKey shelfKey) {
        final String existingId = this.shelfIdsByLocation.get(shelfKey);
        if (existingId != null) {
            return this.shelvesById.get(existingId);
        }
        final String shelfId = UUID.randomUUID().toString();
        final LibraryShelf shelf = new LibraryShelf(shelfId, shelfKey);
        this.shelvesById.put(shelfId, shelf);
        this.shelfIdsByLocation.put(shelfKey, shelfId);
        scheduleSave();
        return shelf;
    }

    public synchronized boolean removeShelfIfEmpty(final ShelfKey shelfKey) {
        final Optional<LibraryShelf> shelfOptional = findByKey(shelfKey);
        if (shelfOptional.isEmpty()) {
            return false;
        }
        final LibraryShelf shelf = shelfOptional.get();
        if (!shelf.isEmpty()) {
            return false;
        }
        this.shelvesById.remove(shelf.shelfId());
        this.shelfIdsByLocation.remove(shelf.key());
        scheduleSave();
        return true;
    }

    public synchronized List<LibraryBook> removeShelf(final ShelfKey shelfKey) {
        final Optional<LibraryShelf> shelfOptional = findByKey(shelfKey);
        if (shelfOptional.isEmpty()) {
            return List.of();
        }
        final LibraryShelf shelf = shelfOptional.get();
        final List<LibraryBook> removedBooks = shelf.removeAllBooks();
        this.shelvesById.remove(shelf.shelfId());
        this.shelfIdsByLocation.remove(shelf.key());
        scheduleSave();
        return removedBooks;
    }

    public synchronized boolean putBook(final String shelfId, final LibraryBook book) {
        final LibraryShelf shelf = this.shelvesById.get(shelfId);
        if (shelf == null || shelf.getBook(book.slotIndex()).isPresent()) {
            return false;
        }
        shelf.putBook(book);
        scheduleSave();
        return true;
    }

    public synchronized Optional<LibraryBook> removeBook(final String shelfId, final int slotIndex) {
        final LibraryShelf shelf = this.shelvesById.get(shelfId);
        if (shelf == null) {
            return Optional.empty();
        }
        final Optional<LibraryBook> removed = shelf.removeBook(slotIndex);
        removed.ifPresent(book -> scheduleSave());
        return removed;
    }

    public synchronized Collection<LibraryShelf> allShelves() {
        return new ArrayList<>(this.shelvesById.values());
    }
}
