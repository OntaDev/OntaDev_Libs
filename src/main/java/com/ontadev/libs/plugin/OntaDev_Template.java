// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.plugin;

import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.metrics.PluginMetrics;
import com.ontadev.libs.service.AddonProvider;
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

    private PluginMetrics metrics;

    protected static Logger log;

    @Override
    public void onLoad() {
        log = LoggerFactory.getLogger(this.getClass());

        pluginIoC = new PluginIoC(this);
    }

    @Override
    public void onDisable() {
        try {
            pluginIoC.shutdownPlugin();
        } finally {
            if (metrics != null) {
                metrics.shutdown();
                metrics = null;
            }
        }
    }


    @Override
    public void onEnable() {
        AddonProvider addons = OntaDev_Libs.getAddonProvider();
        if (addons != null) {
            registerAddons(addons);
            addons.registerIn(pluginIoC);
        }

        registerSharedOrm();

        int bStatsId = getBStatsId();
        if (bStatsId > 0) {
            metrics = new PluginMetrics(this, bStatsId);
            pluginIoC.registerInstance(PluginMetrics.class, metrics);
        }

        preInitializeContainer(pluginIoC);

        initializeContainer(pluginIoC);

        if (metrics != null) {
            registerMetrics(metrics, pluginIoC);
        }

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

    /**
     * Регистрирует API аддонов в общем реестре до инициализации компонентов.
     * Вызывается в onEnable. Найденные API доступны текущему и последующим контейнерам.
     *
     * @param addons общий реестр API
     */
    protected void registerAddons(AddonProvider addons) {}

    /** @return ID плагина на bStats; неположительное значение отключает метрики */
    protected int getBStatsId() {
        return 0;
    }

    /**
     * Добавляет графики после инициализации IoC, перед его onEnable-обработчиками и onPluginEnable.
     * При неположительном ID метод не вызывается.
     * Метрики зарегистрированы в IoC до создания компонентов и доступны через Inject и PostConstruct.
     *
     * @param metrics метрики текущего плагина, также доступные через IoC
     * @param pluginIoC инициализированный контейнер текущего плагина
     */
    protected void registerMetrics(PluginMetrics metrics, PluginIoC pluginIoC) {}

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
