package com.splatage.curatedshelves.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class LibraryItems {
    private LibraryItems() {
    }

    public static ItemStack createLibrariansSeal(final PdcKeys pdcKeys) {
        final ItemStack itemStack = new ItemStack(Material.HONEYCOMB);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Librarian's Seal", NamedTextColor.GOLD));
        itemMeta.lore(List.of(
                Component.text("Marks a chiseled bookshelf", NamedTextColor.GRAY),
                Component.text("as a public Library Shelf.", NamedTextColor.GRAY)
        ));
        itemMeta.getPersistentDataContainer().set(pdcKeys.librarianSeal(), PersistentDataType.BYTE, (byte) 1);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static boolean isLibrariansSeal(final PdcKeys pdcKeys, final ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }
        if (!itemStack.hasItemMeta()) {
            return false;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().has(pdcKeys.librarianSeal(), PersistentDataType.BYTE);
    }

    public static boolean isSupportedBook(final ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.WRITTEN_BOOK;
    }
}
