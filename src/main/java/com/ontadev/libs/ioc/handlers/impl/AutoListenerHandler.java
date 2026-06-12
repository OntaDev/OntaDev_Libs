// OntaDev_Libs Plugin 
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.handlers.impl;

import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.handlers.ClassAnnotationHandler;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

@Slf4j
public class AutoListenerHandler implements ClassAnnotationHandler<AutoListener> {
    private final JavaPlugin plugin;
    private final PluginIoC pluginIoC;

    public AutoListenerHandler(JavaPlugin plugin, PluginIoC pluginIoC) {
        this.plugin = plugin;
        this.pluginIoC = pluginIoC;
    }

    @Override
    public Class<AutoListener> getAnnotation() {
        return AutoListener.class;
    }

    @Override
    public void handle(IoCContainer container, Class<?> clazz, AutoListener annotation) {
        if (!Listener.class.isAssignableFrom(clazz)) {
            log.error("Class {} is not a Listener", clazz.getName());
            throw new IllegalArgumentException("Class " + clazz.getName() + " is not a Listener");
        }

        if (!hasSubscribeMethods(clazz)) {
            log.warn("Class {} marked as @Listener but has no @Subscribe methods", clazz.getName());
        }
    }

    private boolean hasSubscribeMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventHandler.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void postCreate(IoCContainer container, Object instance, AutoListener annotation) {
        pluginIoC.getOnEnable().add(()->{
            Bukkit.getPluginManager().registerEvents((Listener) instance, plugin);

            log.info("Registered listener: {}", instance.getClass().getName());
        });
    }
}