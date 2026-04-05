// PPFSS_Libs Plugin 
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc.handlers.impl;

import com.ppfss.libs.ioc.IoCContainer;
import com.ppfss.libs.ioc.annotation.Listener;
import com.ppfss.libs.ioc.handlers.ClassAnnotationHandler;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
public class ListenerHandler implements ClassAnnotationHandler<Listener> {
    private final ProxyServer proxyServer;
    private final Object plugin;

    public ListenerHandler(ProxyServer proxyServer, Object plugin) {
        this.proxyServer = proxyServer;
        this.plugin = plugin;
    }

    @Override
    public Class<Listener> getAnnotation() {
        return Listener.class;
    }

    @Override
    public void handle(IoCContainer container, Class<?> clazz, Listener annotation) {
        if (!hasSubscribeMethods(clazz)) {
            log.warn("Class {} marked as @Listener but has no @Subscribe methods", clazz.getName());
        }
    }

    private boolean hasSubscribeMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(com.velocitypowered.api.event.Subscribe.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void postCreate(IoCContainer container, Object instance, Listener annotation) {
        proxyServer.getEventManager().register(plugin, instance);

        log.info("Registered listener: {}", instance.getClass().getName());
    }
}