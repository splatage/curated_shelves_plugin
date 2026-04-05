package com.splatage.curatedshelves.platform;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface SchedulerFacade {
    void runGlobal(Runnable task);

    void runAtLocation(Location location, Runnable task);

    void runForPlayer(Player player, Runnable task);

    default void runForPlayer(final Player player, final Runnable task, final Runnable retiredTask) {
        runForPlayer(player, task);
    }

    void runAsync(Runnable task);
}
