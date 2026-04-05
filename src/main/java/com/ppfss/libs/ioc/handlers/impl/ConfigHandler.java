// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc.handlers.impl;

import com.ppfss.libs.config.YamlConfig;
import com.ppfss.libs.config.YamlConfigLoader;
import com.ppfss.libs.ioc.IoCContainer;
import com.ppfss.libs.ioc.annotation.Config;
import com.ppfss.libs.ioc.handlers.ClassAnnotationHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigHandler implements ClassAnnotationHandler<Config> {
    private final YamlConfigLoader configLoader;

    public ConfigHandler(YamlConfigLoader configLoader) {
        this.configLoader = configLoader;
    }


    @Override
    public Class<Config> getAnnotation() {
        return Config.class;
    }

    @Override
    public void handle(IoCContainer container, Class<?> clazz, Config annotation) {
        if (notConfig(clazz)) return;

        int priority = annotation.priority();
        container.registerComponent(clazz, priority, annotation.annotationType());
    }


    @SuppressWarnings("unchecked")
    @Override
    public Object preCreate(IoCContainer container, Class<?> clazz, Config annotation) {
        if (notConfig(clazz)) return null;

        Class<? extends YamlConfig> configClazz = (Class<? extends YamlConfig>) clazz;
        return configLoader.loadFromClass(configClazz);
    }


    private boolean notConfig(Class<?> clazz) {
        if (!YamlConfig.class.isAssignableFrom(clazz)) {
            log.error("Class {} is not a YamlConfig", clazz.getName());
            return true;
        }
        return false;
    }
}