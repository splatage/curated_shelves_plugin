package com.splatage.curatedshelves.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public final class BookActionMenu {
    public static final int READ_SLOT = 3;
    public static final int REMOVE_SLOT = 5;

    private BookActionMenu() {
    }

    public static Inventory create(final UUID shelfId, final UUID bookId) {
        final BookActionMenuHolder holder = new BookActionMenuHolder(shelfId, bookId);
        final Inventory inventory = Bukkit.createInventory(holder, 9, Component.text("Book Actions", NamedTextColor.GOLD));
        holder.inventory(inventory);
        inventory.setItem(READ_SLOT, actionItem(Material.LECTERN, "Read", NamedTextColor.GREEN));
        inventory.setItem(REMOVE_SLOT, actionItem(Material.BARRIER, "Remove", NamedTextColor.RED));
        return inventory;
    }

    private static ItemStack actionItem(final Material material, final String name, final NamedTextColor color) {
        final ItemStack itemStack = new ItemStack(material);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(name, color));
        itemMeta.lore(List.of(Component.text("Click to " + name.toLowerCase() + ".", NamedTextColor.GRAY)));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
