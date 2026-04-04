package com.splatage.curatedshelves.service;

import com.splatage.curatedshelves.util.LibraryItems;
import com.splatage.curatedshelves.util.PdcKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class RecipeService {
    private final Plugin plugin;
    private final PdcKeys pdcKeys;

    public RecipeService(final Plugin plugin, final PdcKeys pdcKeys) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.pdcKeys = Objects.requireNonNull(pdcKeys, "pdcKeys");
    }

    public void registerRecipes() {
        final NamespacedKey recipeKey = new NamespacedKey(this.plugin, "librarians_seal");
        Bukkit.removeRecipe(recipeKey);
        final ShapedRecipe recipe = new ShapedRecipe(recipeKey, LibraryItems.createLibrariansSeal(this.pdcKeys));
        recipe.shape("HCH", "BLB", "HRH");
        recipe.setIngredient('H', Material.HONEYCOMB);
        recipe.setIngredient('C', Material.CANDLE);
        recipe.setIngredient('B', Material.CHISELED_BOOKSHELF);
        recipe.setIngredient('L', Material.LECTERN);
        recipe.setIngredient('R', Material.BARREL);
        Bukkit.addRecipe(recipe);
    }
}
