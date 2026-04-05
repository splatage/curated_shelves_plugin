package com.splatage.curatedshelves.gui;

import com.splatage.curatedshelves.domain.LibraryBook;
import com.splatage.curatedshelves.domain.LibraryShelfSnapshot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class ShelfBrowserMenu {
    public static final int PAGE_SIZE = 45;
    public static final int PREVIOUS_PAGE_SLOT = 45;
    public static final int INFO_SLOT = 49;
    public static final int NEXT_PAGE_SLOT = 53;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withLocale(Locale.ROOT);

    private ShelfBrowserMenu() {
    }

    public static Inventory create(final List<LibraryShelfSnapshot> snapshots, final int requestedPage) {
        final int totalPages = Math.max(1, (int) Math.ceil(snapshots.size() / (double) PAGE_SIZE));
        final int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        final int startIndex = page * PAGE_SIZE;
        final int endIndex = Math.min(startIndex + PAGE_SIZE, snapshots.size());
        final List<LibraryShelfSnapshot> pageSnapshots = snapshots.subList(startIndex, endIndex);

        final List<UUID> shelfIdsBySlot = new ArrayList<>(Collections.nCopies(PAGE_SIZE, null));
        final ShelfBrowserMenuHolder holder = new ShelfBrowserMenuHolder(page, totalPages, shelfIdsBySlot);
        final Inventory inventory = Bukkit.createInventory(holder, 54, Component.text("Curated Shelves", NamedTextColor.GOLD));
        holder.inventory(inventory);

        for (int index = 0; index < pageSnapshots.size(); index++) {
            final LibraryShelfSnapshot snapshot = pageSnapshots.get(index);
            inventory.setItem(index, createShelfItem(snapshot));
            shelfIdsBySlot.set(index, snapshot.shelf().shelfId());
        }

        if (page > 0) {
            inventory.setItem(PREVIOUS_PAGE_SLOT, navigationItem(Material.ARROW, "Previous Page", page, totalPages));
        }
        inventory.setItem(INFO_SLOT, infoItem(snapshots.size(), page, totalPages));
        if (page + 1 < totalPages) {
            inventory.setItem(NEXT_PAGE_SLOT, navigationItem(Material.ARROW, "Next Page", page, totalPages));
        }
        return inventory;
    }

    private static ItemStack createShelfItem(final LibraryShelfSnapshot snapshot) {
        final ItemStack itemStack = new ItemStack(Material.CHISELED_BOOKSHELF);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(displayName(snapshot), NamedTextColor.GOLD));

        final List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Location: " + locationText(snapshot), NamedTextColor.GRAY));
        lore.add(Component.text("Created by: " + snapshot.shelf().createdByName(), NamedTextColor.GRAY));
        lore.add(Component.text("Books: " + snapshot.booksBySlot().size() + " / " + (snapshot.shelf().rows() * 9), NamedTextColor.GRAY));
        lore.add(Component.text("Shelved by: " + shelverSummary(snapshot.booksBySlot().values()), NamedTextColor.GRAY));
        lore.add(Component.text("Created: " + formatTimestamp(snapshot.shelf().createdAt()), NamedTextColor.GRAY));
        lore.add(Component.text("Updated: " + formatTimestamp(snapshot.shelf().updatedAt()), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Click to open this shelf.", NamedTextColor.YELLOW));
        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private static String displayName(final LibraryShelfSnapshot snapshot) {
        return locationText(snapshot);
    }

    private static String locationText(final LibraryShelfSnapshot snapshot) {
        final World world = Bukkit.getWorld(snapshot.shelf().location().worldUuid());
        final String worldName = world != null ? world.getName() : snapshot.shelf().location().worldUuid().toString();
        return worldName + " (" + snapshot.shelf().location().x()
                + ", " + snapshot.shelf().location().y()
                + ", " + snapshot.shelf().location().z() + ")";
    }

    private static String shelverSummary(final Iterable<LibraryBook> books) {
        final Set<String> names = new HashSet<>();
        for (LibraryBook book : books) {
            if (book.shelvedByName() != null && !book.shelvedByName().isBlank()) {
                names.add(book.shelvedByName());
            }
        }
        if (names.isEmpty()) {
            return "None";
        }
        if (names.size() == 1) {
            return names.iterator().next();
        }
        return names.size() + " curators";
    }

    private static String formatTimestamp(final long epochMillis) {
        return TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    private static ItemStack navigationItem(final Material material, final String name, final int page, final int totalPages) {
        final ItemStack itemStack = new ItemStack(material);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(name, NamedTextColor.YELLOW));
        itemMeta.lore(List.of(Component.text("Page " + (page + 1) + " of " + totalPages, NamedTextColor.GRAY)));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private static ItemStack infoItem(final int shelfCount, final int page, final int totalPages) {
        final ItemStack itemStack = new ItemStack(Material.LECTERN);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Shelf Browser", NamedTextColor.GOLD));
        itemMeta.lore(List.of(
                Component.text("Shelves: " + shelfCount, NamedTextColor.GRAY),
                Component.text("Page " + (page + 1) + " of " + totalPages, NamedTextColor.GRAY)
        ));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
