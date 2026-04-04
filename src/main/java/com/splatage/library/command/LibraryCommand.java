package com.splatage.library.command;

import com.splatage.library.platform.SchedulerFacade;
import com.splatage.library.service.LibraryService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LibraryCommand implements CommandExecutor, TabCompleter {
    private final LibraryService libraryService;
    private final SchedulerFacade scheduler;

    public LibraryCommand(final LibraryService libraryService, final SchedulerFacade scheduler) {
        this.libraryService = libraryService;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /library <mark|unmark>", NamedTextColor.RED));
            return true;
        }

        final Block targetBlock = this.targetShelf(player);
        if (targetBlock == null) {
            player.sendMessage(Component.text("Look at a chiseled bookshelf.", NamedTextColor.RED));
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "mark" -> this.mark(player, targetBlock);
            case "unmark" -> this.unmark(player, targetBlock);
            default -> {
                player.sendMessage(Component.text("Usage: /library <mark|unmark>", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean mark(final Player player, final Block block) {
        if (!player.hasPermission("library.admin.mark")) {
            player.sendMessage(Component.text("You do not have permission to mark shelves.", NamedTextColor.RED));
            return true;
        }
        this.scheduler.executeRegion(block.getLocation(), () -> {
            final LibraryService.MarkResult result = this.libraryService.markShelf(block);
            this.scheduler.executePlayer(player, audience -> switch (result) {
                case MARKED -> audience.sendMessage(Component.text("Marked shelf as a library shelf.", NamedTextColor.GREEN));
                case ALREADY_MARKED -> audience.sendMessage(Component.text("That shelf is already a library shelf.", NamedTextColor.YELLOW));
                case INVALID_BLOCK -> audience.sendMessage(Component.text("Only chiseled bookshelves can be marked.", NamedTextColor.RED));
            });
        });
        return true;
    }

    private boolean unmark(final Player player, final Block block) {
        if (!player.hasPermission("library.admin.unmark")) {
            player.sendMessage(Component.text("You do not have permission to unmark shelves.", NamedTextColor.RED));
            return true;
        }
        this.scheduler.executeRegion(block.getLocation(), () -> {
            final LibraryService.UnmarkResult result = this.libraryService.unmarkShelf(block);
            this.scheduler.executePlayer(player, audience -> switch (result) {
                case UNMARKED -> audience.sendMessage(Component.text("Removed library status from the shelf.", NamedTextColor.GREEN));
                case NOT_MARKED -> audience.sendMessage(Component.text("That shelf is not marked as a library shelf.", NamedTextColor.YELLOW));
                case NOT_EMPTY -> audience.sendMessage(Component.text("That shelf still contains books.", NamedTextColor.RED));
            });
        });
        return true;
    }

    private @Nullable Block targetShelf(final Player player) {
        final RayTraceResult result = player.rayTraceBlocks(5.0);
        if (result == null || result.getHitBlock() == null) {
            return null;
        }
        final Block block = result.getHitBlock();
        return block.getType() == Material.CHISELED_BOOKSHELF ? block : null;
    }

    @Override
    public @Nullable List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String alias, final @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("mark", "unmark").stream().filter(option -> option.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
