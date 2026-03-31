// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.command;

import com.velocitypowered.api.command.CommandSource;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;

@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "unused"})
@Getter
@Setter
public abstract class SubCommand {
    private final String name;

    public SubCommand(String name) {
        this.name = name;
    }

    public void execute(CommandSource source, AbstractCommand command, String label, String... args) {
    }

    public List<String> complete(CommandSource source, String... args) {
        return Collections.emptyList();
    }

    public boolean hasPermission(CommandSource source, AbstractCommand command, String label, String... args) {
        String permission = getPermission(source, command, label, args);
        return permission == null || permission.isEmpty() || source.hasPermission(permission);
    }

    public void noPermission(CommandSource source, AbstractCommand command, String label, String... args) {
        source.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
    }

    public String getPermission(CommandSource source, AbstractCommand command, String label, String... args) {
        return null;
    }

    public abstract void sendUsage(CommandSource source, AbstractCommand command, String label, String... args);
}