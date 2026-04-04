package com.splatage.library;

import com.splatage.library.command.LibraryCommand;
import com.splatage.library.config.ConfigLoader;
import com.splatage.library.config.LibraryConfig;
import com.splatage.library.gui.MenuFactory;
import com.splatage.library.listener.LibraryBlockListener;
import com.splatage.library.listener.LibraryMenuListener;
import com.splatage.library.persistence.SQLiteLibraryRepository;
import com.splatage.library.platform.SchedulerFacade;
import com.splatage.library.service.LibraryBadgeService;
import com.splatage.library.service.LibraryService;
import com.splatage.library.util.PdcKeys;
import com.splatage.library.util.SealItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class LibraryPlugin extends JavaPlugin {
    private LibraryConfig libraryConfig;
    private PdcKeys pdcKeys;
    private LibraryService libraryService;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.libraryConfig = ConfigLoader.load(this.getConfig());
        this.pdcKeys = new PdcKeys(this);

        final SchedulerFacade schedulerFacade = new SchedulerFacade(this);
        final SQLiteLibraryRepository repository = new SQLiteLibraryRepository(this.getDataFolder());
        repository.init();

        final LibraryBadgeService badgeService = new LibraryBadgeService(this, this.pdcKeys);
        this.libraryService = new LibraryService(repository, badgeService, this.pdcKeys);
        this.libraryService.loadInitialState();

        final MenuFactory menuFactory = new MenuFactory();
        final LibraryCommand libraryCommand = new LibraryCommand(this.libraryService, schedulerFacade);
        Objects.requireNonNull(this.getCommand("library"), "library command not defined").setExecutor(libraryCommand);
        Objects.requireNonNull(this.getCommand("library"), "library command not defined").setTabCompleter(libraryCommand);

        Bukkit.getPluginManager().registerEvents(new LibraryBlockListener(this.libraryService, menuFactory, this.libraryConfig, this.pdcKeys), this);
        Bukkit.getPluginManager().registerEvents(new LibraryMenuListener(this.libraryService, menuFactory, this.libraryConfig, schedulerFacade), this);
        this.registerSealRecipe();
    }

    private void registerSealRecipe() {
        final NamespacedKey key = new NamespacedKey(this, "librarians_seal");
        final ShapedRecipe recipe = new ShapedRecipe(key, SealItemFactory.create(this.pdcKeys));
        recipe.shape("HCH", "SLS", "HBH");
        recipe.setIngredient('H', org.bukkit.Material.HONEYCOMB);
        recipe.setIngredient('C', org.bukkit.Material.CANDLE);
        recipe.setIngredient('S', org.bukkit.Material.CHISELED_BOOKSHELF);
        recipe.setIngredient('L', org.bukkit.Material.LECTERN);
        recipe.setIngredient('B', org.bukkit.Material.BARREL);
        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);
    }
}
