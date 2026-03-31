// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc;

import com.ppfss.libs.command.AbstractCommand;
import com.ppfss.libs.config.YamlConfig;
import com.ppfss.libs.config.YamlConfigLoader;
import com.ppfss.libs.ioc.handlers.impl.AutoListenerHandler;
import com.ppfss.libs.ioc.handlers.impl.ComponentHandler;
import com.ppfss.libs.ioc.handlers.impl.GsonAdapterHandler;
import com.ppfss.libs.ioc.handlers.impl.InjectFieldHandler;
import com.ppfss.libs.util.AnnotationScanner;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
@SuppressWarnings("unused")
public class PluginIoC {

    /**
     * -- GETTER --
     *  Получить IoCContainer для ручного доступа
     */
    @Getter
    private final IoCContainer container = new IoCContainer();

    private final Object plugin;
    private final ProxyServer proxy;
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
     * @param plugin Экземпляр плагина (главный класс)
     * @param proxy ProxyServer экземпляр
     * @param dataDirectory Директория данных плагина
     * @param pluginContainer PluginContainer плагина
     */
    @SuppressWarnings("unchecked")
    public PluginIoC(Object plugin, ProxyServer proxy, Path dataDirectory, PluginContainer pluginContainer) {
        this.plugin = plugin;
        this.proxy = proxy;

        registerPluginInstance(plugin);
        registerInstance(ProxyServer.class, proxy);
        registerInstance(Path.class, dataDirectory);
        registerInstance(PluginContainer.class, pluginContainer);
        registerInstance(PluginIoC.class, this);

        registerDefaultHandlers();

        Set<Class<?>> classes = AnnotationScanner.scanPlugin(plugin);

        configLoader = new YamlConfigLoader(dataDirectory, container);
        registerInstance(YamlConfigLoader.class, configLoader);

        List<Class<? extends YamlConfig>> configClasses = new ArrayList<>();
        List<Class<? extends AbstractCommand>> commandClasses = new ArrayList<>();

        for (Class<?> clazz : classes) {
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            if (YamlConfig.class.isAssignableFrom(clazz)) {
                configClasses.add((Class<? extends YamlConfig>) clazz);
            }

            if (AbstractCommand.class.isAssignableFrom(clazz)) {
                commandClasses.add((Class<? extends AbstractCommand>) clazz);
            }
        }

        // Регистрируем конфиги
        configClasses.forEach(this::registerYamlConfig);

        // Инициализируем контейнер
        container.initialize(classes);

        // Регистрируем слушателей и команды
        listeners.forEach(this::registerListener);
        commandClasses.forEach(this::registerCommand);
    }

    @SuppressWarnings("unchecked")
    private <T> void registerPluginInstance(T plugin) {
        Class<T> pluginClass = (Class<T>) plugin.getClass();
        registerInstance(pluginClass, plugin);
    }


    /**
     * Создаёт и регистрирует команду через IoC контейнер
     * <p>
     * Если команда уже существует в контейнере — используется существующий instance
     * Если команда ещё не зарегистрирована — выполняется регистрация
     */
    private <T extends AbstractCommand> void registerCommand(Class<T> clazz){
        T command = container.getIfExists(clazz);

        if (command == null){
            command = container.create(clazz);
            container.registerInstance(clazz, command);
        }

        if (!command.isRegistered()){
            command.register(proxy);
        }
    }

    /**
     * <p> Загружает <b>YamlConfig</b> через <b>YamlConfigLoader</b></p>
     * <p> и регистрирует в <b>IoC</b> контейнере </p>
     * <p>
     * Если конфиг уже зарегистрирован — повторная загрузка не выполняется
     */
    private <T extends YamlConfig> void registerYamlConfig(Class<T> clazz) {
        // TODO: LazyLoad
        T config = container.getIfExists(clazz);

        if (config != null) {
            return;
        }

        config = configLoader.loadFromClass(clazz);
        container.registerInstance(clazz, config);
    }

    /**
     * Регистрирует listener в ProxyServer
     */
    private void registerListener(Class<?> clazz) {
        Object listener = container.getIfExists(clazz);

        if (listener != null){
            proxy.getEventManager().register(plugin, listener);
            log.info("Registered listener for plugin {}", clazz.getName());
        }
    }

    /**
     * Регистрация стандартных обработчиков аннотаций
     */
    private void registerDefaultHandlers() {
        // Классы
        container.registerClassHandler(new ComponentHandler());
        container.registerClassHandler(new GsonAdapterHandler());
        container.registerClassHandler(new AutoListenerHandler(listeners));

        // Поля
        container.registerFieldHandler(new InjectFieldHandler());
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