package com.splatage.curatedshelves;

import com.splatage.curatedshelves.command.LibraryCommand;
import com.splatage.curatedshelves.config.PluginSettings;
import com.splatage.curatedshelves.data.ShelfStore;
import com.splatage.curatedshelves.listener.LibraryInventoryListener;
import com.splatage.curatedshelves.listener.ShelfDestroyListener;
import com.splatage.curatedshelves.listener.ShelfInteractListener;
import com.splatage.curatedshelves.platform.SchedulerFacade;
import com.splatage.curatedshelves.service.BadgeService;
import com.splatage.curatedshelves.service.LibraryService;
import com.splatage.curatedshelves.util.LibraryItems;
import com.splatage.curatedshelves.util.PdcKeys;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CuratedShelvesPlugin extends JavaPlugin {
    private PluginSettings settings;
    private SchedulerFacade schedulerFacade;
    private ShelfStore shelfStore;
    private BadgeService badgeService;
    private LibraryService libraryService;
    private PdcKeys pdcKeys;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();

        this.schedulerFacade = new SchedulerFacade(this);
        this.pdcKeys = new PdcKeys(this);
        this.shelfStore = new ShelfStore(this, this.schedulerFacade);
        this.shelfStore.load();
        this.badgeService = new BadgeService(this, this.pdcKeys);
        this.libraryService = new LibraryService(this, this.schedulerFacade, this.shelfStore, this.badgeService, this.pdcKeys, this.settings);

        LibraryItems.registerSealRecipe(this, this.pdcKeys);
        registerCommands();
        registerListeners();
        this.libraryService.rebuildVisibleBadges();
    }

    @Override
    public void onDisable() {
        if (this.shelfStore != null) {
            this.shelfStore.saveBlocking();
        }
    }

    private void registerCommands() {
        final PluginCommand libraryCommand = Objects.requireNonNull(getCommand("library"), "library command missing from plugin.yml");
        final LibraryCommand executor = new LibraryCommand(this::reloadRuntimeConfig, this.libraryService);
        libraryCommand.setExecutor(executor);
        libraryCommand.setTabCompleter(executor);
    }

    private void registerListeners() {
        final var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new ShelfInteractListener(this.libraryService, this.pdcKeys), this);
        pluginManager.registerEvents(new LibraryInventoryListener(this.libraryService), this);
        pluginManager.registerEvents(new ShelfDestroyListener(this.libraryService), this);
    }

    private void reloadRuntimeConfig() {
        reloadPluginConfig();
        this.libraryService.updateSettings(this.settings);
    }

    private void reloadPluginConfig() {
        reloadConfig();
        this.settings = PluginSettings.fromConfig(getConfig());
        if (getConfig().getInt("rows", 1) != this.settings.rows()) {
            getLogger().warning("rows must be between 1 and 4; clamped to " + this.settings.rows());
        }
    }
}
