package com.splatage.library.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class BookItems {
    private BookItems() {
    }

    public static boolean isWrittenBook(final ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.WRITTEN_BOOK && itemStack.getItemMeta() instanceof BookMeta;
    }

    public static String titleOf(final ItemStack itemStack) {
        if (!(itemStack.getItemMeta() instanceof BookMeta bookMeta)) {
            return "Untitled Book";
        }
        final String title = bookMeta.getTitle();
        return title == null || title.isBlank() ? "Untitled Book" : title;
    }

    public static String authorOf(final ItemStack itemStack) {
        if (!(itemStack.getItemMeta() instanceof BookMeta bookMeta)) {
            return "Unknown";
        }
        final String author = bookMeta.getAuthor();
        return author == null || author.isBlank() ? "Unknown" : author;
    }

    public static boolean canDepositByAuthorRule(final ItemStack itemStack, final Player player) {
        final String author = authorOf(itemStack);
        return author.equalsIgnoreCase(player.getName());
    }

    public static ItemStack menuDisplay(final @NotNull ItemStack itemStack, final String shelvedByName) {
        final ItemStack display = itemStack.clone();
        final ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            final List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Author: ", NamedTextColor.GRAY)
                    .append(Component.text(authorOf(itemStack), NamedTextColor.WHITE)));
            lore.add(Component.text("Shelved by: ", NamedTextColor.GRAY)
                    .append(Component.text(shelvedByName, NamedTextColor.WHITE)));
            meta.lore(lore);
            meta.displayName(Component.text(titleOf(itemStack), NamedTextColor.GOLD));
            display.setItemMeta(meta);
        }
        return display;
    }

    public static ItemStack singleCopy(final @NotNull ItemStack itemStack) {
        final ItemStack copy = itemStack.clone();
        copy.setAmount(1);
        return copy;
    }
}
