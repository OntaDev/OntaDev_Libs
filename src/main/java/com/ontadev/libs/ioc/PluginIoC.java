// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc;

import com.ontadev.libs.config.SettingsConfig;
import com.ontadev.libs.config.YamlConfigLoader;
import com.ontadev.libs.ioc.handlers.impl.*;
import com.ontadev.libs.menu.manager.MenuManager;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.orm.mapper.EntityMapper;
import com.ontadev.libs.orm.OrmRepositoryHandler;
import com.ontadev.libs.orm.adapter.EntityRowMapperFactory;
import com.ontadev.libs.orm.adapter.JsonTypeAdapter;
import com.ontadev.libs.orm.adapter.UuidTypeAdapter;
import com.ontadev.libs.orm.database.DatabaseManager;
import com.ontadev.libs.orm.database.Dialect;
import com.ontadev.libs.orm.entity.SchemaGenerator;
import com.ontadev.libs.serialization.GsonAdapter;
import com.ontadev.libs.serialization.adapters.ComponentAdapter;
import com.ontadev.libs.serialization.adapters.EnumSetAdapter;
import com.ontadev.libs.serialization.adapters.MessageAdapter;
import com.ontadev.libs.util.AnnotationScanner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jdbi.v3.core.Jdbi;

import java.lang.reflect.Modifier;
import java.util.*;


@Slf4j
@SuppressWarnings("unused")
public class PluginIoC {

    @Getter
    private final List<Runnable> onEnable = new ArrayList<>();
    @Getter
    private final IoCContainer container = new IoCContainer();

    private final JavaPlugin plugin;
    private final ShutdownHandler shutdownHandler;
    private final Set<Class<?>> listeners = new HashSet<>();
    private final YamlConfigLoader configLoader;
    private MenuManager menuManager;

    private Set<Class<?>> tempClasses;

    /**
     * Инициализирует IoC контейнер для плагина
     * <p>
     * Выполняет:<p>
     * - Регистрацию базовых зависимостей (ProxyServer, PluginContainer, Path)<p>
     * - Сканирование классов плагина<p>
     * - Регистрацию YamlConfig<p>
     * - Инициализацию IoC контейнера<p>
     * - Регистрацию Listener и Command<p>
     * <p>
     *
     * @param plugin Экземпляр плагина (главный класс)
     */
    public PluginIoC(JavaPlugin plugin) {
        this.plugin = plugin;

        registerDefaultInstance(plugin);

        shutdownHandler = new ShutdownHandler();

        Set<Class<?>> classes = AnnotationScanner.scanPlugin(plugin);

        configLoader = loadConfigLoader(classes);

        registerDefaultHandlers();

        tempClasses = classes;
    }

    /**
     * То же самое, что {@link #PluginIoC(JavaPlugin)}, но без {@link AnnotationScanner#scanPlugin}:
     * набор классов передаётся готовым. Нужен для сценариев, где классы плагина
     * не могут быть найдены обычным сканированием classpath'а - сам источник набора вне зоны
     * ответственности этой библиотеки, она лишь принимает готовый результат.
     */
    public PluginIoC(JavaPlugin plugin, Set<Class<?>> classes) {
        this.plugin = plugin;

        registerDefaultInstance(plugin);

        shutdownHandler = new ShutdownHandler();

        configLoader = loadConfigLoader(classes);

        registerDefaultHandlers();

        tempClasses = classes;
    }

    private void registerOrm(){
        SettingsConfig config = this.configLoader.loadFromClass(SettingsConfig.class);

        container.registerInstance(SettingsConfig.class, config);

        DatabaseManager databaseManager = new DatabaseManager(config);

        container.registerInstance(DatabaseManager.class, databaseManager);
        container.registerInstance(Jdbi.class, databaseManager.getJdbi());
        container.registerInstance(Dialect.class, databaseManager.getDialect());

        new UuidTypeAdapter().registerOn(databaseManager.getJdbi());
        new JsonTypeAdapter().registerOn(databaseManager.getJdbi());

        EntityMapper entityMapper = new EntityMapper(databaseManager.getDialect());
        container.registerInstance(EntityMapper.class, entityMapper);

        SchemaGenerator generator = new SchemaGenerator(databaseManager, entityMapper);
        container.registerInstance(SchemaGenerator.class, generator);

        EntityRowMapperFactory entityRowMapperFactory = new EntityRowMapperFactory(entityMapper);
        databaseManager.getJdbi().registerRowMapper(entityRowMapperFactory);

        container.registerInterfaceHandler(new OrmRepositoryHandler());
    }

    public void initializeContainer() {
        if (tempClasses == null) return;

        container.initialize(tempClasses);

        tempClasses = null;
    }

    private void registerDefaultInstance(JavaPlugin plugin) {
        registerPluginInstance(plugin);

        registerInstance(JavaPlugin.class, plugin);
        registerInstance(Plugin.class, plugin);
        registerInstance(Server.class, plugin.getServer());
        registerInstance(IoCContainer.class, container);
        registerInstance(PluginIoC.class, this);
    }


    private YamlConfigLoader loadConfigLoader(Set<Class<?>> classes) {
        Map<Class<?>, Object> adapters = scanForAdapters(classes);

        registerDefaultAdapters(adapters, container);

        return createConfigLoader(adapters);
    }

    private YamlConfigLoader createConfigLoader(Map<Class<?>, Object> adapters) {
        YamlConfigLoader loader = new YamlConfigLoader(plugin.getDataFolder().toPath(), adapters);

        registerInstance(YamlConfigLoader.class, loader);

        return loader;
    }

    private Map<Class<?>, Object> scanForAdapters(Set<Class<?>> classes) {
        Map<Class<?>, Object> adapters = new HashMap<>();

        for (Class<?> clazz : classes) {
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            GsonAdapter gsonAdapter = clazz.getAnnotation(GsonAdapter.class);

            if (gsonAdapter == null) {
                continue;
            }

            if (clazz.isAnnotation()) {
                continue;
            }

            log.info("Registering adapter for {}", clazz.getName());

            Class<?> type = gsonAdapter.value();
            Object adapter = container.create(clazz);

            adapters.put(type, adapter);
        }

        return adapters;
    }

    @SuppressWarnings("unchecked")
    private <T> void registerPluginInstance(T plugin) {
        Class<T> pluginClass = (Class<T>) plugin.getClass();
        registerInstance(pluginClass, plugin);
    }

    private boolean isConcreteClass(Class<?> clazz) {
        return !clazz.isInterface()
                && !clazz.isAnnotation()
                && !clazz.isEnum()
//                && !clazz.isRecord()
                && !Modifier.isAbstract(clazz.getModifiers());
    }

    private void registerDefaultAdapters(Map<Class<?>, Object> adapters, IoCContainer container) {
        adapters.put(Message.class, container.create(MessageAdapter.class));
        adapters.put(EnumSet.class, container.create(EnumSetAdapter.class));
        adapters.put(Component.class, container.create(ComponentAdapter.class));
    }

    /**
     * Регистрация стандартных обработчиков аннотаций
     */
    private void registerDefaultHandlers() {
        // Классы
        container.registerClassHandler(new ConfigHandler(configLoader));
        container.registerClassHandler(new RepositoryHandler());
        container.registerClassHandler(new ComponentHandler());
        container.registerClassHandler(new ServiceHandler());
        container.registerClassHandler(new CommandHandler(plugin));
        container.registerClassHandler(new AutoListenerHandler(plugin));
        container.registerClassHandler(new MenuHandler(menuManager));


        // Поля
        container.registerFieldHandler(new InjectFieldHandler());

        // Методы
        container.registerMethodHandler(shutdownHandler);
    }

    public void shutdownPlugin() {
        shutdownHandler.runAll();
    }

    /**
     * Получить instance класса из IoC
     */
    public <T> T get(Class<T> type) {
        return container.get(type);
    }

    /**
     * Зарегистрировать вручную конкретный компонент
     */
    @Deprecated
    public void registerComponent(Class<?> clazz) {
        container.registerComponent(clazz);
    }

    /**
     * Зарегистрировать реализацию интерфейса
     */
    public <T> void registerImplementation(Class<T> type, Class<? extends T> impl) {
        container.registerImplementation(type, impl);
    }

    /**
     * Зарегистрировать уже созданный instance
     */
    public <T> void registerInstance(Class<T> type, T instance) {
        container.registerInstance(type, instance);
    }

    public void onEnable() {
        onEnable.forEach(Runnable::run);
    }
}