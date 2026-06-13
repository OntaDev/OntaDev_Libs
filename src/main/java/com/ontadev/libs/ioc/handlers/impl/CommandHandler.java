// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.handlers.impl;

import com.ontadev.libs.command.AbstractCommand;
import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.ioc.annotation.stereotype.Command;
import com.ontadev.libs.ioc.handlers.ClassAnnotationHandler;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.java.JavaPlugin;

@Slf4j
public class CommandHandler implements ClassAnnotationHandler<Command> {
    private final JavaPlugin plugin;

    public CommandHandler(JavaPlugin plugin) {
        this.plugin = plugin;
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

        command.register(plugin);
        log.info("Command {} registered", command);
    }
}