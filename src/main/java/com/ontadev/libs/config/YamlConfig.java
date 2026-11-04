// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT

package com.ontadev.libs.config;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

/**
 * Базовый класс YAML-конфига, привязываемого к файлу через {@link YamlConfigLoader}.
 * Статические и transient-поля не сохраняются.
 */
@SuppressWarnings("unused")
@Setter
@Getter
public abstract class YamlConfig {
    private transient YamlConfigLoader configLoader = null;
    private transient Path file;

    /** Создаёт конфиг без привязки к файлу и загрузчику. */
    public YamlConfig() {}

    /**
     * Сохраняет текущие значения, сохраняя существующие YAML-комментарии и неизвестные ключи.
     * Если загрузчик не назначен, ничего не делает.
     *
     * @throws RuntimeException если сохранить файл не удалось
     */
    public void save(){
        if (configLoader == null) return;
        configLoader.saveConfig(this);
    }

    /**
     * Перечитывает файл и обновляет поля текущего экземпляра.
     * Если загрузчик не назначен, ничего не делает.
     * При ошибке чтения или разбора YAML текущие значения остаются прежними.
     *
     * @throws RuntimeException если загрузить файл не удалось
     */
    public void reload() {
        if (configLoader == null) return;
        configLoader.reloadConfig(this);
    }

    /**
     * Копирует значения полей из совместимого конфига, включая унаследованные поля.
     * Ссылочные значения копируются без клонирования; static, transient и final-поля
     * пропускаются. Недоступные для записи поля остаются без изменений.
     *
     * @param other источник значений; null и несовместимые типы игнорируются
     */
    public void applyFrom(YamlConfig other) {
        if (other == null) return;
        Class<?> clazz = other.getClass();
        if (!clazz.isAssignableFrom(this.getClass()) && !this.getClass().isAssignableFrom(clazz)) {
            return;
        }
        Class<?> current = other.getClass();
        while (current != null && YamlConfig.class.isAssignableFrom(current)) {
            for (var field : current.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (java.lang.reflect.Modifier.isStatic(mods) || java.lang.reflect.Modifier.isTransient(mods)) {
                    continue;
                }
                if (java.lang.reflect.Modifier.isFinal(mods)) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(other);
                    field.set(this, value);
                } catch (IllegalAccessException ignored) {}
            }
            current = current.getSuperclass();
        }
    }


    /**
     * Возвращает имя файла относительно каталога конфигов загрузчика.
     *
     * @return имя файла; если суффикс {@code .yml} отсутствует, загрузчик добавит его
     */
    public abstract String getFileName();
}
