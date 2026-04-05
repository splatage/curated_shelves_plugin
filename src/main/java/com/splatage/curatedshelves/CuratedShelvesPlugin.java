package com.splatage.curatedshelves;

import com.splatage.curatedshelves.command.LibraryCommand;
import com.splatage.curatedshelves.config.ConfigLoader;
import com.splatage.curatedshelves.config.PluginConfig;
import com.splatage.curatedshelves.data.LibraryRepository;
import com.splatage.curatedshelves.data.MySqlLibraryRepository;
import com.splatage.curatedshelves.data.SQLiteLibraryRepository;
import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.listener.InventoryListener;
import com.splatage.curatedshelves.listener.ShelfDestroyListener;
import com.splatage.curatedshelves.listener.ShelfInteractListener;
import com.splatage.curatedshelves.platform.PaperFoliaSchedulerFacade;
import com.splatage.curatedshelves.platform.SchedulerFacade;
import com.splatage.curatedshelves.service.BadgeService;
import com.splatage.curatedshelves.service.LibraryService;
import com.splatage.curatedshelves.service.RecipeService;
import com.splatage.curatedshelves.service.ShelfMarkerService;
import com.splatage.curatedshelves.util.PdcKeys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class CuratedShelvesPlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private SchedulerFacade schedulerFacade;
    private PdcKeys pdcKeys;
    private LibraryService libraryService;
    private ShelfMarkerService shelfMarkerService;
    private BadgeService badgeService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();
        this.schedulerFacade = new PaperFoliaSchedulerFacade(this);
        this.pdcKeys = new PdcKeys(this);
        this.shelfMarkerService = new ShelfMarkerService(this.pdcKeys);
        this.badgeService = new BadgeService(this.pdcKeys);

        try {
            Files.createDirectories(getDataFolder().toPath());
            final LibraryRepository repository = createRepository();
            this.libraryService = new LibraryService(this, this.schedulerFacade, repository);
            this.libraryService.initialize();
        } catch (final IOException | SQLException exception) {
            throw new IllegalStateException("Failed to initialize CuratedShelves storage", exception);
        }

        final RecipeService recipeService = new RecipeService(this, this.pdcKeys);
        recipeService.registerRecipes();

        getServer().getPluginManager().registerEvents(
                new ShelfInteractListener(this, this.libraryService, this.shelfMarkerService, this.badgeService),
                this
        );
        getServer().getPluginManager().registerEvents(new InventoryListener(this, this.libraryService), this);
        getServer().getPluginManager().registerEvents(
                new ShelfDestroyListener(this, this.libraryService, this.shelfMarkerService, this.badgeService),
                this
        );

        final PluginCommand libraryCommand = getCommand("library");
        if (libraryCommand == null) {
            throw new IllegalStateException("Command 'library' is not defined in plugin.yml");
        }
        final LibraryCommand commandExecutor = new LibraryCommand(this, this.libraryService, this.shelfMarkerService, this.badgeService);
        libraryCommand.setExecutor(commandExecutor);
        libraryCommand.setTabCompleter(commandExecutor);

        reconcileLoadedShelves();

        getLogger().info("CuratedShelves enabled.");
        getLogger().info("Configured library rows: " + this.pluginConfig.rows());
        getLogger().info("Configured storage backend: " + this.pluginConfig.storage().type());
        getLogger().info("Configured storage table prefix: '" + this.pluginConfig.storage().tablePrefix() + "'");
    }

    @Override
    public void onDisable() {
        if (this.libraryService != null) {
            this.libraryService.shutdown();
        }
        getLogger().info("CuratedShelves disabled.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.pluginConfig = ConfigLoader.load(getConfig());
    }

    public PluginConfig pluginConfig() {
        return this.pluginConfig;
    }

    public SchedulerFacade schedulerFacade() {
        return this.schedulerFacade;
    }

    public PdcKeys pdcKeys() {
        return this.pdcKeys;
    }

    public LibraryService libraryService() {
        return this.libraryService;
    }

    public ShelfMarkerService shelfMarkerService() {
        return this.shelfMarkerService;
    }

    public BadgeService badgeService() {
        return this.badgeService;
    }

    private LibraryRepository createRepository() {
        final PluginConfig.StorageConfig storage = this.pluginConfig.storage();
        return switch (storage.type()) {
            case SQLITE -> {
                final Path databasePath = getDataFolder().toPath().resolve(storage.sqlite().file());
                yield new SQLiteLibraryRepository(databasePath, storage.tablePrefix());
            }
            case MYSQL -> new MySqlLibraryRepository(
                    storage.mysql().host(),
                    storage.mysql().port(),
                    storage.mysql().database(),
                    storage.mysql().username(),
                    storage.mysql().password(),
                    storage.mysql().maximumPoolSize(),
                    storage.mysql().connectionTimeoutMillis(),
                    storage.tablePrefix()
            );
        };
    }

    private void reconcileLoadedShelves() {
        final List<LibraryShelf> shelves = List.copyOf(this.libraryService.allShelves());
        for (LibraryShelf shelf : shelves) {
            final World world = Bukkit.getWorld(shelf.location().worldUuid());
            if (world == null) {
                deleteReconciledShelf(shelf, "world is unavailable");
                continue;
            }
            final Location location = new Location(world, shelf.location().x(), shelf.location().y(), shelf.location().z());
            this.schedulerFacade.runAtLocation(location, () -> reconcileShelfAtLocation(shelf, location));
        }
    }

    private void reconcileShelfAtLocation(final LibraryShelf shelf, final Location location) {
        final Block block = location.getBlock();
        if (!this.shelfMarkerService.isEligibleBlock(block)) {
            deleteReconciledShelf(shelf, "backing block is missing or no longer a chiseled bookshelf");
            return;
        }
        final Optional<UUID> markerShelfId = this.shelfMarkerService.shelfId(block);
        if (markerShelfId.isPresent() && markerShelfId.get().equals(shelf.shelfId())) {
            return;
        }
        this.badgeService.removeBadge(block, shelf.shelfId());
        if (markerShelfId.isPresent()) {
            this.badgeService.removeBadge(block, markerShelfId.get());
            this.shelfMarkerService.unmark(block);
        }
        deleteReconciledShelf(shelf, "marker is missing or no longer matches persisted shelf state");
    }

    private void deleteReconciledShelf(final LibraryShelf shelf, final String reason) {
        this.libraryService.deleteShelf(
                shelf.shelfId(),
                () -> getLogger().warning("Removed inconsistent Library Shelf " + shelf.shelfId() + ": " + reason),
                throwable -> {
                    this.libraryService.discardShelfRuntime(shelf.shelfId());
                    getLogger().log(Level.SEVERE, "Failed to remove inconsistent Library Shelf " + shelf.shelfId(), throwable);
                }
        );
    }
}
