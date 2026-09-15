// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT
package com.ontadev.libs.plugin;

import com.ontadev.libs.config.SettingsConfig;
import com.ontadev.libs.config.YamlConfigLoader;
import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.menu.MenuManagerImpl;
import com.ontadev.libs.menu.manager.MenuManager;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.message.MessageDispatcher;
import com.ontadev.libs.orm.database.DatabaseManager;
import com.ontadev.libs.player.PlayerResolver;
import lombok.Getter;

@SuppressWarnings("unused")
public final class OntaDev_Libs extends OntaDev_Template {
    @Getter
    public static OntaDev_Libs instance;

    public static MenuManager defaultMenuManager;
    public static PlayerResolver playerResolver;


    public static DatabaseManager databaseManager;

    @Override
    public void onLoad() {
        Message.load(this);
        MessageDispatcher.load(this);

        super.onLoad();
    }

    @Override
    public void onEnable() {
        createDefaultMenuManager();
        registerMenuManager();

        super.onEnable();
    }

    private void createDefaultMenuManager() {
        IoCContainer container = getPluginIoC().getContainer();
        playerResolver = container.create(PlayerResolver.class);
        defaultMenuManager = container.create(MenuManagerImpl.class);
    }

    private void loadDefaultConfig(PluginIoC ioC){
        YamlConfigLoader configLoader = ioC.get(YamlConfigLoader.class);

        SettingsConfig config = configLoader.loadFromClass(SettingsConfig.class);
        ioC.registerInstance(SettingsConfig.class, config);
    }

    private void loadDatabase(PluginIoC ioC){
        DatabaseManager databaseManager = ioC.get(DatabaseManager.class);

        ioC.registerInstance(DatabaseManager.class, databaseManager);

        OntaDev_Libs.databaseManager = databaseManager;
    }

    private void registerMenuManager(){
        PluginIoC ioc = getPluginIoC();
        ioc.registerInstance(MenuManager.class, defaultMenuManager);
    }

    @Override
    public void onPluginEnable(PluginIoC pluginIoC) {
        log.info("[OntaDevLibs] Enabled");
    }

}
