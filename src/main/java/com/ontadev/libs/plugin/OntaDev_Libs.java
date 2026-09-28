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
import com.ontadev.libs.orm.OrmRepositoryHandler;
import com.ontadev.libs.orm.adapter.EntityRowMapperFactory;
import com.ontadev.libs.orm.database.DatabaseManager;
import com.ontadev.libs.orm.entity.SchemaGenerator;
import com.ontadev.libs.orm.mapper.EntityMapper;
import com.ontadev.libs.player.PlayerResolver;
import lombok.Getter;

@SuppressWarnings("unused")
public final class OntaDev_Libs extends OntaDev_Template {
    @Getter
    public static OntaDev_Libs instance;

    public static MenuManager defaultMenuManager;
    public static PlayerResolver playerResolver;

    /**
     * Общие (единые для всех плагинов на этой библиотеке) ORM-компоненты.
     * Собираются один раз здесь и прокидываются в IoC каждого плагина
     * в {@link OntaDev_Template#onEnable()}.
     */
    public static DatabaseManager databaseManager;
    public static EntityMapper entityMapper;
    public static SchemaGenerator schemaGenerator;
    public static OrmRepositoryHandler ormRepositoryHandler;

    @Override
    public void onLoad() {
        Message.load(this);
        MessageDispatcher.load(this);

        super.onLoad();
    }

    @Override
    public void onEnable() {
        loadDatabase();

        createDefaultMenuManager();
        registerMenuManager();

        super.onEnable();
    }

    private void createDefaultMenuManager() {
        IoCContainer container = getPluginIoC().getContainer();
        playerResolver = container.create(PlayerResolver.class);
        defaultMenuManager = container.create(MenuManagerImpl.class);
    }

    private void loadDatabase(){
        PluginIoC ioC = getPluginIoC();

        YamlConfigLoader loader = ioC.get(YamlConfigLoader.class);
        SettingsConfig settingsConfig = loader.loadFromClass(SettingsConfig.class);
        ioC.registerInstance(SettingsConfig.class, settingsConfig); // Регистрация конфига

        String configuredUrl = settingsConfig.getDatabaseSettings().getUrl();
        boolean urlWasMissing = configuredUrl == null || configuredUrl.isBlank();

        DatabaseManager databaseManager = new DatabaseManager(settingsConfig); // применит SQLite fallback при необходимости

        if (urlWasMissing) {
            settingsConfig.save(); // фиксируем в конфиге URL, на который откатились
        }

        EntityMapper entityMapper = new EntityMapper(databaseManager.getDialect());
        SchemaGenerator schemaGenerator = new SchemaGenerator(databaseManager, entityMapper);

        databaseManager.getJdbi().registerRowMapper(new EntityRowMapperFactory(entityMapper));

        OntaDev_Libs.databaseManager = databaseManager;
        OntaDev_Libs.entityMapper = entityMapper;
        OntaDev_Libs.schemaGenerator = schemaGenerator;
        OntaDev_Libs.ormRepositoryHandler = new OrmRepositoryHandler();
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
