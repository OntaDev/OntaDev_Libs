package com.ontadev.libs.service;

import com.ontadev.libs.exception.AddonNotFoundException;
import com.ontadev.libs.ioc.PluginIoC;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Общий реестр найденных API аддонов. Используется в основном потоке сервера. */
public class AddonProvider {
    private final ServicesManager services;
    private final Map<Class<?>, Object> addons = new LinkedHashMap<>();

    /** @param services реестр сервисов Bukkit для поиска API */
    public AddonProvider(ServicesManager services) {
        this.services = Objects.requireNonNull(services);
    }

    /**
     * Находит API через supplier и кеширует непустой результат по типу.
     * Для найденного типа supplier повторно не вызывается.
     *
     * @param supplier источник API
     * @param clazz тип API для регистрации в IoC
     * @param required выбрасывать исключение, если API не найдено
     * @param <T> тип API
     * @throws AddonNotFoundException если обязательное API не найдено
     */
    public <T> void registerAddon(Supplier<? extends T> supplier, Class<T> clazz, boolean required) {
        if (addons.containsKey(clazz)) return;
        T addon = supplier.get();
        if (addon == null) {
            if (required) throw new AddonNotFoundException(clazz.getName() + " not found");
            return;
        }
        addons.put(clazz, clazz.cast(addon));
    }

    /**
     * Находит API через Bukkit ServicesManager и кеширует найденный экземпляр.
     *
     * @param clazz тип API
     * @param name имя аддона для сообщения об ошибке
     * @param required выбрасывать исключение, если сервис не найден
     * @param <T> тип API
     * @throws AddonNotFoundException если обязательный сервис не найден
     */
    public <T> void registerAddon(Class<T> clazz, String name, boolean required) {
        registerAddon(() -> {
            RegisteredServiceProvider<T> provider = services.getRegistration(clazz);
            T addon = provider == null ? null : provider.getProvider();
            if (addon == null && required) throw new AddonNotFoundException(name + " not found");
            return addon;
        }, clazz, required);
    }

    /**
     * Регистрирует найденные API и этот реестр в контейнере.
     * Ранее заполненные контейнеры автоматически не обновляются.
     *
     * @param pluginIoC контейнер получателя
     */
    public void registerIn(PluginIoC pluginIoC) {
        pluginIoC.registerInstance(AddonProvider.class, this);
        addons.forEach((type, addon) -> registerInstance(pluginIoC, type, addon));
    }

    private <T> void registerInstance(PluginIoC pluginIoC, Class<T> type, Object addon) {
        pluginIoC.registerInstance(type, type.cast(addon));
    }
}
