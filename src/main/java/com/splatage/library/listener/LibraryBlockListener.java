package com.splatage.library.listener;

import com.splatage.library.config.LibraryConfig;
import com.splatage.library.gui.MenuFactory;
import com.splatage.library.service.LibraryService;
import com.splatage.library.util.SealItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public final class LibraryBlockListener implements Listener {
    private final LibraryService libraryService;
    private final MenuFactory menuFactory;
    private final LibraryConfig config;
    private final com.splatage.library.util.PdcKeys keys;

    public LibraryBlockListener(
            final LibraryService libraryService,
            final MenuFactory menuFactory,
            final LibraryConfig config,
            final com.splatage.library.util.PdcKeys keys
    ) {
        this.libraryService = libraryService;
        this.menuFactory = menuFactory;
        this.config = config;
        this.keys = keys;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHISELED_BOOKSHELF) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (SealItemFactory.isSeal(itemInHand, this.keys)) {
            event.setCancelled(true);
            final LibraryService.MarkResult result = this.libraryService.markShelf(block);
            switch (result) {
                case MARKED -> {
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        itemInHand.subtract();
                    }
                    player.sendMessage(Component.text("The shelf has been marked as a library shelf.", NamedTextColor.GREEN));
                }
                case ALREADY_MARKED -> player.sendMessage(Component.text("That shelf is already a library shelf.", NamedTextColor.YELLOW));
                case INVALID_BLOCK -> player.sendMessage(Component.text("Only chiseled bookshelves can be marked.", NamedTextColor.RED));
            }
            return;
        }

        final Optional<com.splatage.library.domain.LibraryShelfRecord> shelf = this.libraryService.resolveShelf(block);
        if (shelf.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        this.libraryService.ensureBadge(block);
        player.openInventory(this.menuFactory.libraryMenu(shelf.get(), this.libraryService.booksFor(shelf.get().shelfId()), this.config));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(final BlockBreakEvent event) {
        this.spill(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(final BlockBurnEvent event) {
        this.spill(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        for (final Block block : List.copyOf(event.blockList())) {
            this.spill(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        for (final Block block : List.copyOf(event.blockList())) {
            this.spill(block);
        }
    }

    private void spill(final Block block) {
        if (this.libraryService.readShelfId(block).isEmpty()) {
            return;
        }
        for (final ItemStack itemStack : this.libraryService.spillShelf(block)) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), itemStack);
        }
    }
}
