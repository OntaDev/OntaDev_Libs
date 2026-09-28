// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.plugin;

import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.menu.MenuManagerImpl;
import com.ontadev.libs.menu.manager.MenuManager;
import com.ontadev.libs.orm.database.DatabaseManager;
import com.ontadev.libs.orm.database.Dialect;
import com.ontadev.libs.orm.entity.SchemaGenerator;
import com.ontadev.libs.orm.mapper.EntityMapper;
import com.ontadev.libs.player.PlayerResolver;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jdbi.v3.core.Jdbi;
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
        registerSharedOrm();

        preInitializeContainer(pluginIoC);

        initializeContainer(pluginIoC);

        pluginIoC.onEnable();

        onPluginEnable(pluginIoC);
    }

    /**
     * Прокидывает общий (единый на весь сервер) набор ORM-компонентов,
     * собранный в {@link OntaDev_Libs#databaseManager} и соседних static-полях,
     * в IoC-контейнер текущего плагина. Без загруженного OntaDev_Libs ORM просто недоступен.
     */
    protected void registerSharedOrm() {
        DatabaseManager databaseManager = OntaDev_Libs.databaseManager;

        if (databaseManager == null) {
            log.warn("Общий DatabaseManager ещё не готов, ORM для {} будет недоступен", getName());
            return;
        }

        pluginIoC.registerInstance(DatabaseManager.class, databaseManager);
        pluginIoC.registerInstance(Jdbi.class, databaseManager.getJdbi());
        pluginIoC.registerInstance(Dialect.class, databaseManager.getDialect());
        pluginIoC.registerInstance(EntityMapper.class, OntaDev_Libs.entityMapper);
        pluginIoC.registerInstance(SchemaGenerator.class, OntaDev_Libs.schemaGenerator);

        pluginIoC.getContainer().registerInterfaceHandler(OntaDev_Libs.ormRepositoryHandler);
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
