package com.splatage.curatedshelves.gui;

import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelfSnapshot;
import com.splatage.curatedshelves.util.BookCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class LibraryMenu {
    private LibraryMenu() {
    }

    public static Inventory create(final LibraryShelfSnapshot snapshot) {
        final LibraryMenuHolder holder = new LibraryMenuHolder(snapshot.shelf().shelfId());
        final Inventory inventory = Bukkit.createInventory(holder, snapshot.shelf().rows() * 9, Component.text("Library Shelf", NamedTextColor.GOLD));
        holder.inventory(inventory);

        for (LibraryBook book : snapshot.booksBySlot().values()) {
            inventory.setItem(book.slotIndex(), createDisplayItem(book));
        }
        return inventory;
    }

    public static ItemStack createDisplayItem(final LibraryBook book) {
        final ItemStack itemStack = BookCodec.deserializeItem(book.serializedItem()).clone();
        final ItemMeta itemMeta = itemStack.getItemMeta();
        final List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Title: " + book.title(), NamedTextColor.GRAY));
        lore.add(Component.text("Author: " + valueOrUnknown(book.author()), NamedTextColor.GRAY));
        lore.add(Component.text("Shelved by: " + book.shelvedByName(), NamedTextColor.GRAY));
        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private static String valueOrUnknown(final String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }
}
