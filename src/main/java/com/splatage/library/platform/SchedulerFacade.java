package com.splatage.library.platform;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class SchedulerFacade {
    private final Plugin plugin;

    public SchedulerFacade(final Plugin plugin) {
        this.plugin = plugin;
    }

    public void executeRegion(final @NotNull Location location, final @NotNull Runnable runnable) {
        Bukkit.getServer().getRegionScheduler().execute(this.plugin, location, runnable);
    }

    public void executePlayer(final @NotNull Player player, final @NotNull Consumer<Player> consumer) {
        player.getScheduler().run(this.plugin, scheduledTask -> consumer.accept(player), null);
    }

    public void executePlayerDelayed(final @NotNull Player player, final long delayTicks, final @NotNull Consumer<Player> consumer) {
        player.getScheduler().runDelayed(this.plugin, scheduledTask -> consumer.accept(player), null, delayTicks);
    }

    public ScheduledTask executeAsync(final @NotNull Runnable runnable) {
        return Bukkit.getServer().getAsyncScheduler().runNow(this.plugin, scheduledTask -> runnable.run());
    }

    public ScheduledTask executeAsyncDelayed(final long delay, final @NotNull TimeUnit unit, final @NotNull Runnable runnable) {
        return Bukkit.getServer().getAsyncScheduler().runDelayed(this.plugin, scheduledTask -> runnable.run(), delay, unit);
    }
}
