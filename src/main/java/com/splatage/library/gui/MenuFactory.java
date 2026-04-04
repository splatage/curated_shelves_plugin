package com.splatage.library.gui;

import com.splatage.library.config.LibraryConfig;
import com.splatage.library.domain.LibraryBookRecord;
import com.splatage.library.domain.LibraryShelfRecord;
import com.splatage.library.util.BookItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MenuFactory {
    public Inventory libraryMenu(
            final @NotNull LibraryShelfRecord shelf,
            final @NotNull List<LibraryBookRecord> books,
            final @NotNull LibraryConfig config
    ) {
        final LibraryMenuHolder holder = new LibraryMenuHolder(shelf.shelfId());
        final Inventory inventory = Bukkit.createInventory(holder, config.slotCount(), Component.text("Library Shelf", NamedTextColor.DARK_AQUA));
        holder.inventory(inventory);
        for (final LibraryBookRecord book : books) {
            if (book.slotIndex() < 0 || book.slotIndex() >= inventory.getSize()) {
                continue;
            }
            inventory.setItem(book.slotIndex(), BookItems.menuDisplay(book.item(), book.shelvedByName()));
        }
        return inventory;
    }

    public Inventory actionMenu(final @NotNull LibraryBookRecord book) {
        final BookActionMenuHolder holder = new BookActionMenuHolder(book.shelfId(), book.slotIndex());
        final Inventory inventory = Bukkit.createInventory(holder, 9, Component.text("Book Actions", NamedTextColor.DARK_AQUA));
        holder.inventory(inventory);
        inventory.setItem(3, this.action(Material.LECTERN, "Read", NamedTextColor.GREEN));
        inventory.setItem(4, BookItems.menuDisplay(book.item(), book.shelvedByName()));
        inventory.setItem(5, this.action(Material.BARRIER, "Remove", NamedTextColor.RED));
        return inventory;
    }

    private ItemStack action(final Material material, final String name, final NamedTextColor color) {
        final ItemStack itemStack = ItemStack.of(material);
        final ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text(name, color));
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
