package com.splatage.curatedshelves.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class SchedulerFacade {
    private final Plugin plugin;

    public SchedulerFacade(final Plugin plugin) {
        this.plugin = plugin;
    }

    public void executeRegion(final Location location, final Runnable runnable) {
        this.plugin.getServer().getRegionScheduler().execute(this.plugin, location, runnable);
    }

    public void executeEntity(final Entity entity, final Runnable runnable) {
        entity.getScheduler().execute(this.plugin, runnable, null, 1L);
    }

    public void executeAsync(final Runnable runnable) {
        this.plugin.getServer().getAsyncScheduler().runNow(this.plugin, task -> runnable.run());
    }
}
