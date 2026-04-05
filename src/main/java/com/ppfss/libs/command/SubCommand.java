package com.ppfss.libs.command;

import com.velocitypowered.api.command.CommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class SubCommand {

    @NotNull
    public abstract String getName();

    public boolean canSee(@NotNull CommandSource source) {
        return true;
    }

    public boolean canExecute(
            @NotNull CommandSource source,
            @NotNull String[] args
    ) {
        return true;
    }

    public void noPermission(
            @NotNull CommandSource source,
            @NotNull AbstractCommand command,
            @NotNull String label,
            @NotNull String[] args
    ) {
    }

    public abstract void execute(
            @NotNull CommandSource source,
            @NotNull AbstractCommand command,
            @NotNull String label,
            @NotNull String[] args
    );

    @NotNull
    public List<String> complete(
            @NotNull CommandSource source,
            @NotNull String[] args
    ) {
        return Collections.emptyList();
    }
}