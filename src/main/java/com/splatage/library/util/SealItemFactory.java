package com.splatage.library.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SealItemFactory {
    private SealItemFactory() {
    }

    public static ItemStack create(final @NotNull PdcKeys keys) {
        final ItemStack itemStack = ItemStack.of(Material.HONEYCOMB);
        final ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Librarian's Seal", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Use on a chiseled bookshelf", NamedTextColor.GRAY),
                Component.text("to consecrate it as a library shelf.", NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(keys.librarianSeal, PersistentDataType.BYTE, (byte) 1);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static boolean isSeal(final ItemStack itemStack, final @NotNull PdcKeys keys) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }
        final ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(keys.librarianSeal, PersistentDataType.BYTE);
    }
}
