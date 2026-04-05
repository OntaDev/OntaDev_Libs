// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc.handlers.impl;

import com.ppfss.libs.command.AbstractCommand;
import com.ppfss.libs.ioc.IoCContainer;
import com.ppfss.libs.ioc.annotation.Command;
import com.ppfss.libs.ioc.handlers.ClassAnnotationHandler;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandHandler implements ClassAnnotationHandler<Command> {
    private final ProxyServer proxyServer;

    public CommandHandler(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }


    @Override
    public Class<Command> getAnnotation() {
        return Command.class;
    }

    @Override
    public void handle(IoCContainer container, Class<?> clazz, Command annotation) {
        if (!AbstractCommand.class.isAssignableFrom(clazz)) {
            log.error("Class {} is not a Command", clazz.getName());
            return;
        }

        int priority = annotation.priority();
        container.registerComponent(clazz, priority, annotation.annotationType());
    }

    @Override
    public void postCreate(IoCContainer container, Object instance, Command annotation) {
        AbstractCommand command = (AbstractCommand) instance;

        command.register(proxyServer);
        log.info("Command {} registered", command);
    }
}