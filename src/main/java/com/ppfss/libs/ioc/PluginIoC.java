// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc;

import com.ppfss.libs.config.YamlConfigLoader;
import com.ppfss.libs.ioc.handlers.impl.*;
import com.ppfss.libs.message.Message;
import com.ppfss.libs.serialization.GsonAdapter;
import com.ppfss.libs.serialization.adapters.ComponentAdapter;
import com.ppfss.libs.serialization.adapters.EnumSetAdapter;
import com.ppfss.libs.serialization.adapters.MessageAdapter;
import com.ppfss.libs.util.AnnotationScanner;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;


@Slf4j
@SuppressWarnings("unused")
public class PluginIoC {

    /**
     * -- GETTER --
     * Получить IoCContainer для ручного доступа
     */
    @Getter
    private final IoCContainer container = new IoCContainer();

    private final Object plugin;
    private final ProxyServer proxy;
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
     * @param plugin          Экземпляр плагина (главный класс)
     * @param proxy           ProxyServer экземпляр
     * @param dataDirectory   Директория данных плагина
     * @param pluginContainer PluginContainer плагина
     */
    public PluginIoC(Object plugin, ProxyServer proxy, Path dataDirectory, PluginContainer pluginContainer) {
        this.plugin = plugin;
        this.proxy = proxy;

        registerPluginInstance(plugin);
        registerInstance(ProxyServer.class, proxy);
        registerInstance(Path.class, dataDirectory);
        registerInstance(PluginContainer.class, pluginContainer);
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


        configLoader = new YamlConfigLoader(dataDirectory, adapters);

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
                && !clazz.isRecord()
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
        container.registerClassHandler(new CommandHandler(proxy));
        container.registerClassHandler(new ListenerHandler(proxy, plugin));

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
}