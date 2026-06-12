// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT

package com.ontadev.libs.config;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@SuppressWarnings("unused")
@Setter
@Getter
public abstract class YamlConfig {
    private transient YamlConfigLoader configLoader = null;
    private transient Path file;

    public YamlConfig() {}

    public void save(){
        if (configLoader == null) return;
        configLoader.saveConfig(this);
    }

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


    public abstract String getFileName();
}