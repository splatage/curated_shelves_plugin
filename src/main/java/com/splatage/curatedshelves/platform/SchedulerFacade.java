package com.splatage.curatedshelves.platform;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface SchedulerFacade {
    void runGlobal(Runnable task);

    void runAtLocation(Location location, Runnable task);

    void runForPlayer(Player player, Runnable task);

    void runAsync(Runnable task);
}
