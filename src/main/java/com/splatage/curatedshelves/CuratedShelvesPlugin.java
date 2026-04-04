package com.splatage.curatedshelves;

import com.splatage.curatedshelves.config.ConfigLoader;
import com.splatage.curatedshelves.config.PluginConfig;
import com.splatage.curatedshelves.platform.PaperFoliaSchedulerFacade;
import com.splatage.curatedshelves.platform.SchedulerFacade;
import org.bukkit.plugin.java.JavaPlugin;

public final class CuratedShelvesPlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private SchedulerFacade schedulerFacade;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = ConfigLoader.load(getConfig());
        this.schedulerFacade = new PaperFoliaSchedulerFacade(this);

        getLogger().info("CuratedShelves bootstrap slice enabled.");
        getLogger().info("Configured library rows: " + this.pluginConfig.rows());
    }

    @Override
    public void onDisable() {
        getLogger().info("CuratedShelves bootstrap slice disabled.");
    }

    public PluginConfig pluginConfig() {
        return this.pluginConfig;
    }

    public SchedulerFacade schedulerFacade() {
        return this.schedulerFacade;
    }
}
