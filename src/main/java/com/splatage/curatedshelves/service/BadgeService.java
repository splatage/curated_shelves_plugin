package com.splatage.curatedshelves.service;

import com.splatage.curatedshelves.data.LibraryShelf;
import com.splatage.curatedshelves.util.PdcKeys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class BadgeService {
    private final Plugin plugin;
    private final PdcKeys keys;

    public BadgeService(final Plugin plugin, final PdcKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    public void spawnOrRefreshBadge(final LibraryShelf shelf) {
        final World world = Bukkit.getWorld(shelf.key().worldUuid());
        if (world == null) {
            return;
        }
        final Block block = world.getBlockAt(shelf.key().x(), shelf.key().y(), shelf.key().z());
        final ItemDisplay existing = findBadge(block, shelf.shelfId());
        if (existing != null) {
            configureBadge(existing, block, shelf.shelfId());
            return;
        }
        world.spawn(computeBadgeLocation(block), ItemDisplay.class, display -> configureBadge(display, block, shelf.shelfId()));
    }

    public void removeBadge(final LibraryShelf shelf) {
        final World world = Bukkit.getWorld(shelf.key().worldUuid());
        if (world == null) {
            return;
        }
        final Block block = world.getBlockAt(shelf.key().x(), shelf.key().y(), shelf.key().z());
        final ItemDisplay existing = findBadge(block, shelf.shelfId());
        if (existing != null) {
            existing.remove();
        }
    }

    private ItemDisplay findBadge(final Block block, final String shelfId) {
        for (final Entity entity : block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 1.0, 1.0, 1.0)) {
            if (!(entity instanceof ItemDisplay display)) {
                continue;
            }
            final Byte marker = display.getPersistentDataContainer().get(this.keys.badgeMarker(), PersistentDataType.BYTE);
            final String storedShelfId = display.getPersistentDataContainer().get(this.keys.shelfId(), PersistentDataType.STRING);
            if (marker != null && marker == (byte) 1 && Objects.equals(shelfId, storedShelfId)) {
                return display;
            }
        }
        return null;
    }

    private void configureBadge(final ItemDisplay display, final Block block, final String shelfId) {
        display.setItemStack(new ItemStack(Material.LECTERN));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setBillboard(Display.Billboard.FIXED);
        display.setGravity(false);
        display.setInvulnerable(true);
        display.setPersistent(true);
        display.setSilent(true);
        display.getPersistentDataContainer().set(this.keys.badgeMarker(), PersistentDataType.BYTE, (byte) 1);
        display.getPersistentDataContainer().set(this.keys.shelfId(), PersistentDataType.STRING, shelfId);
        display.teleport(computeBadgeLocation(block));
    }

    private Location computeBadgeLocation(final Block block) {
        double x = block.getX() + 0.5;
        double y = block.getY() + 0.48;
        double z = block.getZ() + 0.5;
        if (block.getBlockData() instanceof Directional directional) {
            x += directional.getFacing().getModX() * 0.36;
            z += directional.getFacing().getModZ() * 0.36;
        }
        return new Location(block.getWorld(), x, y, z);
    }
}
