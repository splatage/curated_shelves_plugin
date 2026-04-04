package com.splatage.curatedshelves.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class BookCodec {
    private BookCodec() {
    }

    public static String serializeItem(final ItemStack itemStack) {
        try {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream objectOutputStream = new BukkitObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(itemStack);
            }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to serialize item stack", exception);
        }
    }

    public static ItemStack deserializeItem(final String encoded) {
        try {
            final byte[] data = Base64.getDecoder().decode(encoded);
            try (BukkitObjectInputStream objectInputStream = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
                return (ItemStack) objectInputStream.readObject();
            }
        } catch (final IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Failed to deserialize item stack", exception);
        }
    }
}
