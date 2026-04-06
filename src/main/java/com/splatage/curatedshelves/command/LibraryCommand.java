package com.splatage.curatedshelves.command;

import com.splatage.curatedshelves.CuratedShelvesPlugin;
import com.splatage.curatedshelves.domain.LibraryShelf;
import com.splatage.curatedshelves.domain.LocationKey;
import com.splatage.curatedshelves.gui.LibraryViews;
import com.splatage.curatedshelves.service.BadgeService;
import com.splatage.curatedshelves.service.LibraryService;
import com.splatage.curatedshelves.service.ShelfMarkerService;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class LibraryCommand implements CommandExecutor, TabCompleter {
    private final CuratedShelvesPlugin plugin;
    private final LibraryService libraryService;
    private final ShelfMarkerService shelfMarkerService;
    private final BadgeService badgeService;

    public LibraryCommand(
            final CuratedShelvesPlugin plugin,
            final LibraryService libraryService,
            final ShelfMarkerService shelfMarkerService,
            final BadgeService badgeService
    ) {
        this.plugin = plugin;
        this.libraryService = libraryService;
        this.shelfMarkerService = shelfMarkerService;
        this.badgeService = badgeService;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /library <mark|unmark|reload|browse>");
            return true;
        }
        return switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "mark" -> handleMark(sender);
            case "unmark" -> handleUnmark(sender);
            case "reload" -> handleReload(sender);
            case "browse" -> handleBrowse(sender);
            default -> {
                sender.sendMessage("Usage: /library <mark|unmark|reload|browse>");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return List.of("mark", "unmark", "reload", "browse").stream()
                    .filter(entry -> entry.startsWith(args[0].toLowerCase(java.util.Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }

    private boolean handleMark(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use /library mark.");
            return true;
        }
        if (!player.hasPermission("curatedshelves.admin.mark")) {
            player.sendMessage("You do not have permission to mark Library Shelves.");
            return true;
        }
        final Block block = targetShelfBlock(player);
        if (block == null) {
            player.sendMessage("Look at a chiseled bookshelf.");
            return true;
        }
        if (this.shelfMarkerService.isMarked(block)) {
            player.sendMessage("That shelf is already a Library Shelf.");
            return true;
        }
        if (this.shelfMarkerService.hasPhysicalContents(block)) {
            player.sendMessage("That shelf must be empty before it can be marked as a Library Shelf.");
            return true;
        }
        final LibraryShelf shelf = this.libraryService.newShelf(LocationKey.fromLocation(block.getLocation()), this.plugin.pluginConfig().rows(), player);
        this.libraryService.createShelf(
                shelf,
                () -> this.plugin.schedulerFacade().runAtLocation(block.getLocation(), () -> {
                    if (!this.shelfMarkerService.isEligibleBlock(block)) {
                        this.libraryService.deleteUnboundShelf(shelf.shelfId(), () -> { }, deleteFailure ->
                                this.plugin.getLogger().log(Level.SEVERE, "Failed to clean up unbound Library Shelf", deleteFailure)
                        );
                        this.plugin.schedulerFacade().runForPlayer(player, () -> player.sendMessage("The target shelf changed before it could be marked."));
                        return;
                    }
                    try {
                        this.shelfMarkerService.mark(block, shelf.shelfId());
                        this.badgeService.ensureBadge(block, shelf);
                        if (!this.libraryService.activateCreatedShelf(shelf.shelfId())) {
                            this.shelfMarkerService.unmark(block);
                            this.badgeService.removeBadge(block, shelf.shelfId());
                            this.libraryService.deleteUnboundShelf(shelf.shelfId(), () -> { }, deleteFailure ->
                                    this.plugin.getLogger().log(Level.SEVERE, "Failed to clean up unbound Library Shelf", deleteFailure)
                            );
                            this.plugin.schedulerFacade().runForPlayer(player, () -> player.sendMessage("The Library Shelf could not be finalized."));
                            return;
                        }
                        this.plugin.schedulerFacade().runForPlayer(player, () -> player.sendMessage("Library Shelf marked."));
                    } catch (final Throwable throwable) {
                        this.shelfMarkerService.unmark(block);
                        this.badgeService.removeBadge(block, shelf.shelfId());
                        this.libraryService.deleteUnboundShelf(shelf.shelfId(), () -> { }, deleteFailure ->
                                this.plugin.getLogger().log(Level.SEVERE, "Failed to clean up unbound Library Shelf", deleteFailure)
                        );
                        this.plugin.getLogger().log(Level.SEVERE, "Failed to finalize Library Shelf binding", throwable);
                        this.plugin.schedulerFacade().runForPlayer(player, () -> player.sendMessage("Failed to mark that shelf."));
                    }
                }),
                throwable -> this.plugin.schedulerFacade().runForPlayer(player, () -> {
                    this.plugin.getLogger().log(Level.SEVERE, "Failed to mark Library Shelf", throwable);
                    player.sendMessage("Failed to mark that shelf.");
                })
        );
        return true;
    }

    private boolean handleUnmark(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use /library unmark.");
            return true;
        }
        if (!player.hasPermission("curatedshelves.admin.unmark")) {
            player.sendMessage("You do not have permission to unmark Library Shelves.");
            return true;
        }
        final Block block = targetShelfBlock(player);
        if (block == null || !this.shelfMarkerService.isMarked(block)) {
            player.sendMessage("Look at a marked Library Shelf.");
            return true;
        }
        final UUID shelfId = this.shelfMarkerService.shelfId(block).orElse(null);
        if (shelfId == null || this.libraryService.shelfById(shelfId).isEmpty()) {
            this.plugin.schedulerFacade().runAtLocation(block.getLocation(), () -> {
                if (shelfId != null) {
                    this.badgeService.removeBadge(block, shelfId);
                }
                this.shelfMarkerService.unmark(block);
                this.plugin.schedulerFacade().runForPlayer(player, () ->
                        player.sendMessage("Invalid Library Shelf marker removed."));
            });
            return true;
        }
        final var snapshot = this.libraryService.snapshotIfPresent(shelfId);
        if (snapshot.isEmpty()) {
            this.plugin.schedulerFacade().runAtLocation(block.getLocation(), () -> {
                this.badgeService.removeBadge(block, shelfId);
                this.shelfMarkerService.unmark(block);
                this.plugin.schedulerFacade().runForPlayer(player, () ->
                        player.sendMessage("Invalid Library Shelf marker removed."));
            });
            return true;
        }
        if (!snapshot.get().booksBySlot().isEmpty()) {
            player.sendMessage("That Library Shelf must be empty before it can be unmarked.");
            return true;
        }
        if (!this.libraryService.beginShelfRemoval(shelfId)) {
            player.sendMessage("That Library Shelf is already being removed.");
            return true;
        }
        this.plugin.schedulerFacade().runAtLocation(block.getLocation(), () -> {
            this.shelfMarkerService.unmark(block);
            this.badgeService.removeBadge(block, shelfId);
            this.libraryService.deleteShelf(
                    shelfId,
                    () -> this.plugin.schedulerFacade().runForPlayer(player, () -> player.sendMessage("Library Shelf unmarked.")),
                    throwable -> {
                        this.libraryService.discardShelfRuntime(shelfId);
                        this.plugin.getLogger().log(
                                Level.SEVERE,
                                "Failed to unmark Library Shelf " + shelfId
                                        + " at " + snapshot.get().shelf().location()
                                        + "; runtime state discarded and persisted cleanup may remain until the next startup reconciliation.",
                                throwable
                        );
                        this.plugin.schedulerFacade().runForPlayer(player, () ->
                                player.sendMessage("Failed to unmark that shelf. Runtime state discarded; persisted cleanup may remain until restart."));
                    }
            );
        });
        return true;
    }

    private boolean handleReload(final CommandSender sender) {
        if (!sender.hasPermission("curatedshelves.admin.reload")) {
            sender.sendMessage("You do not have permission to reload CuratedShelves.");
            return true;
        }
        try {
            this.plugin.reloadPluginConfig();
            sender.sendMessage("CuratedShelves configuration reloaded. Storage backend and table-prefix changes require a restart to take effect.");
        } catch (final IllegalArgumentException exception) {
            sender.sendMessage("CuratedShelves configuration reload failed: " + exception.getMessage());
        }
        return true;
    }

    private boolean handleBrowse(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use /library browse.");
            return true;
        }
        if (!player.hasPermission("curatedshelves.admin.browse")
                && !player.hasPermission("curatedshelves.admin.edit")) {
            player.sendMessage("You do not have permission to browse CuratedShelves.");
            return true;
        }
        LibraryViews.openShelfBrowserMenu(player, this.libraryService.allShelfSnapshotsSorted(), 0);
        return true;
    }

    private Block targetShelfBlock(final Player player) {
        final Block block = player.getTargetBlockExact(6, FluidCollisionMode.NEVER);
        if (block == null || block.getType() != Material.CHISELED_BOOKSHELF) {
            return null;
        }
        return block;
    }
}
