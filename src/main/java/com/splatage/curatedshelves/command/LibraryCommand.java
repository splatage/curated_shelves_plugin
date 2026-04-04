package com.splatage.curatedshelves.command;

import com.splatage.curatedshelves.config.PluginSettings;
import com.splatage.curatedshelves.service.LibraryService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LibraryCommand implements CommandExecutor, TabCompleter {
    private final Runnable reloadAction;
    private final LibraryService libraryService;

    public LibraryCommand(final Runnable reloadAction, final LibraryService libraryService) {
        this.reloadAction = reloadAction;
        this.libraryService = libraryService;
    }

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final String[] args
    ) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /library <mark|unmark|reload>");
            return true;
        }
        final String subcommand = args[0].toLowerCase(java.util.Locale.ROOT);
        switch (subcommand) {
            case "mark" -> handleMark(sender);
            case "unmark" -> handleUnmark(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage("Usage: /library <mark|unmark|reload>");
        }
        return true;
    }

    private void handleMark(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can mark library shelves.");
            return;
        }
        if (!player.hasPermission("curatedshelves.admin.mark")) {
            sender.sendMessage("You do not have permission to mark library shelves.");
            return;
        }
        final Block target = player.getTargetBlockExact(6);
        if (target == null || target.getType() != Material.CHISELED_BOOKSHELF) {
            sender.sendMessage("Look at a chiseled bookshelf within 6 blocks.");
            return;
        }
        if (this.libraryService.isLibraryShelf(target)) {
            sender.sendMessage("That shelf is already marked as a Library Shelf.");
            return;
        }
        this.libraryService.markShelf(target).ifPresentOrElse(
                shelf -> sender.sendMessage("Library Shelf created."),
                () -> sender.sendMessage("That block cannot be marked as a Library Shelf.")
        );
    }

    private void handleUnmark(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can unmark library shelves.");
            return;
        }
        if (!player.hasPermission("curatedshelves.admin.unmark")) {
            sender.sendMessage("You do not have permission to unmark library shelves.");
            return;
        }
        final Block target = player.getTargetBlockExact(6);
        if (target == null || target.getType() != Material.CHISELED_BOOKSHELF) {
            sender.sendMessage("Look at a chiseled bookshelf within 6 blocks.");
            return;
        }
        if (!this.libraryService.isLibraryShelf(target)) {
            sender.sendMessage("That shelf is not a Library Shelf.");
            return;
        }
        if (!this.libraryService.unmarkShelfIfEmpty(target)) {
            sender.sendMessage("That Library Shelf is not empty. Remove or break it first.");
            return;
        }
        sender.sendMessage("Library Shelf removed.");
    }

    private void handleReload(final CommandSender sender) {
        if (!sender.hasPermission("curatedshelves.admin.reload")) {
            sender.sendMessage("You do not have permission to reload Curated Shelves.");
            return;
        }
        this.reloadAction.run();
        sender.sendMessage("Curated Shelves configuration reloaded.");
    }

    @Override
    public @Nullable List<String> onTabComplete(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String alias,
            final String[] args
    ) {
        if (args.length == 1) {
            return List.of("mark", "unmark", "reload").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(java.util.Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
