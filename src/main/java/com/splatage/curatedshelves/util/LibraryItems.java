package com.splatage.curatedshelves.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class LibraryItems {
    private LibraryItems() {
    }

    public static ItemStack createLibrariansSeal(final PdcKeys keys) {
        final ItemStack itemStack = new ItemStack(Material.HONEYCOMB);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName("§6Librarian's Seal");
        itemMeta.setLore(List.of(
                "§7Use on a chiseled bookshelf",
                "§7to mark it as a Library Shelf."
        ));
        itemMeta.getPersistentDataContainer().set(keys.sealMarker(), PersistentDataType.BYTE, (byte) 1);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static boolean isLibrariansSeal(final ItemStack itemStack, final PdcKeys keys) {
        if (itemStack == null || itemStack.getType() != Material.HONEYCOMB || !itemStack.hasItemMeta()) {
            return false;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().has(keys.sealMarker(), PersistentDataType.BYTE);
    }

    public static void registerSealRecipe(final Plugin plugin, final PdcKeys keys) {
        final ItemStack result = createLibrariansSeal(keys);
        final NamespacedKey recipeKey = new NamespacedKey(plugin, "librarians_seal");
        final ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("HCH", "SLS", "HBH");
        recipe.setIngredient('H', Material.HONEYCOMB);
        recipe.setIngredient('C', Material.CANDLE);
        recipe.setIngredient('S', Material.CHISELED_BOOKSHELF);
        recipe.setIngredient('L', Material.LECTERN);
        recipe.setIngredient('B', Material.BARREL);
        plugin.getServer().addRecipe(recipe);
    }
}
