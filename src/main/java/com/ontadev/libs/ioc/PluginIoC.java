// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc;

import com.ontadev.libs.config.YamlConfigLoader;
import com.ontadev.libs.ioc.handlers.impl.*;
import com.ontadev.libs.message.Message;
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

import java.lang.reflect.Modifier;
import java.util.*;


@Slf4j
@SuppressWarnings("unused")
public class PluginIoC {

    @Getter
    private final List<Runnable> onEnable = new ArrayList<>();
    /**
     * -- GETTER --
     * Получить IoCContainer для ручного доступа
     */
    @Getter
    private final IoCContainer container = new IoCContainer();

    private final JavaPlugin plugin;
    private final ShutdownHandler shutdownHandler;
    private final Set<Class<?>> listeners = new HashSet<>();
    private final YamlConfigLoader configLoader;

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

        registerPluginInstance(plugin);
        registerInstance(JavaPlugin.class, plugin);
        registerInstance(Plugin.class, plugin);
        registerInstance(IoCContainer.class, container);
        registerInstance(Server.class, plugin.getServer());
        registerInstance(PluginIoC.class, this);

        shutdownHandler = new ShutdownHandler();

        Set<Class<?>> classes = AnnotationScanner.scanPlugin(plugin);

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

        registerDefaultAdapters(adapters, container);


        configLoader = new YamlConfigLoader(plugin.getDataFolder().toPath(), adapters);

        registerInstance(YamlConfigLoader.class, configLoader);

        registerDefaultHandlers();

        // Инициализируем контейнер
        container.initialize(classes);

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
        container.registerClassHandler(new AutoListenerHandler(plugin, this));

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