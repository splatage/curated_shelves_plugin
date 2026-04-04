package com.splatage.curatedshelves.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ItemSerialization {
    private ItemSerialization() {
    }

    public static String serialize(final ItemStack itemStack) {
        try {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream outputStream = new BukkitObjectOutputStream(byteArrayOutputStream)) {
                outputStream.writeObject(itemStack);
            }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to serialize ItemStack", exception);
        }
    }

    public static ItemStack deserialize(final String serialized) {
        try {
            final byte[] bytes = Base64.getDecoder().decode(serialized);
            try (BukkitObjectInputStream inputStream = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                final Object value = inputStream.readObject();
                if (!(value instanceof ItemStack itemStack)) {
                    throw new IllegalStateException("Serialized value was not an ItemStack");
                }
                return itemStack;
            }
        } catch (final IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Failed to deserialize ItemStack", exception);
        }
    }
}
