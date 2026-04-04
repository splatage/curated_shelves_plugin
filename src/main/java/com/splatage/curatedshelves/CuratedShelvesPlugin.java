package com.splatage.curatedshelves;

import com.splatage.curatedshelves.command.LibraryCommand;
import com.splatage.curatedshelves.config.ConfigLoader;
import com.splatage.curatedshelves.config.PluginConfig;
import com.splatage.curatedshelves.data.LibraryRepository;
import com.splatage.curatedshelves.data.SQLiteLibraryRepository;
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
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

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
            final Path databasePath = getDataFolder().toPath().resolve("library.db");
            final LibraryRepository repository = new SQLiteLibraryRepository(databasePath);
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

        getLogger().info("CuratedShelves enabled.");
        getLogger().info("Configured library rows: " + this.pluginConfig.rows());
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
}
