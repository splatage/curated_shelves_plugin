package com.splatage.curatedshelves.platform;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class PaperFoliaSchedulerFacade implements SchedulerFacade {
    private final Plugin plugin;

    public PaperFoliaSchedulerFacade(final Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void runGlobal(final Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(this.plugin, scheduledTask -> task.run());
    }

    @Override
    public void runAtLocation(final Location location, final Runnable task) {
        Objects.requireNonNull(location, "location");
        Bukkit.getRegionScheduler().run(this.plugin, location, scheduledTask -> task.run());
    }

    @Override
    public void runForPlayer(final Player player, final Runnable task) {
        Objects.requireNonNull(player, "player");
        player.getScheduler().run(this.plugin, scheduledTask -> task.run(), null);
    }

    @Override
    public void runAsync(final Runnable task) {
        Bukkit.getAsyncScheduler().runNow(this.plugin, scheduledTask -> task.run());
    }
}
