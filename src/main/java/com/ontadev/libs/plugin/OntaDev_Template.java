// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.plugin;

import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.menu.MenuManagerImpl;
import com.ontadev.libs.menu.manager.MenuManager;
import com.ontadev.libs.player.PlayerResolver;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OntaDev_Template extends JavaPlugin {

    @Getter
    private PluginIoC pluginIoC;

    protected static Logger log;

    @Override
    public void onLoad() {
        log = LoggerFactory.getLogger(this.getClass());

        pluginIoC = new PluginIoC(this);
    }

    @Override
    public void onDisable() {
        pluginIoC.shutdownPlugin();
    }


    @Override
    public void onEnable() {
        preInitializeContainer(pluginIoC);

        initializeContainer(pluginIoC);

        pluginIoC.onEnable();

        onPluginEnable(pluginIoC);
    }

    protected void preInitializeContainer(PluginIoC pluginIoC){
        registerMenuManager(pluginIoC);
    }

    protected void registerMenuManager(PluginIoC pluginIoC){
        pluginIoC.registerInstance(PlayerResolver.class, OntaDev_Libs.playerResolver);

        pluginIoC.registerInstance(MenuManager.class, OntaDev_Libs.defaultMenuManager);
        pluginIoC.registerInstance(MenuManagerImpl.class,(MenuManagerImpl) OntaDev_Libs.defaultMenuManager);
    }

    protected void initializeContainer(PluginIoC pluginIoC){
        pluginIoC.initializeContainer();
    }

    public abstract void onPluginEnable(PluginIoC pluginIoC);
}
