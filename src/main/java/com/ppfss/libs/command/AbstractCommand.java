// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public abstract class AbstractCommand {

    protected final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    @Getter
    private boolean registered = false;

    public void registerSubCommand(@NotNull SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    public void register(@NotNull ProxyServer proxy) {

        if (registered) {
            throw new IllegalStateException(
                    "Command '" + getName() + "' already registered"
            );
        }

        LiteralArgumentBuilder<CommandSource> root =
                LiteralArgumentBuilder.<CommandSource>literal(getName())
                        .executes(this::executeRoot);

        for (SubCommand sub : subCommands.values()) {

            LiteralArgumentBuilder<CommandSource> subBuilder =
                    LiteralArgumentBuilder.<CommandSource>literal(sub.getName())
                            .requires(sub::canSee)
                            .executes(context ->
                                    executeSub(context, sub, new String[0])
                            );

            subBuilder.then(
                    ArgumentBuilderFactory.greedyArgs(
                            context -> executeSub(
                                    context,
                                    sub,
                                    ArgumentBuilderFactory.getArgs(context)
                            ),
                            context -> sub.complete(
                                    context.getSource(),
                                    ArgumentBuilderFactory.getArgs(context)
                            )
                    )
            );

            root.then(subBuilder);
        }

        BrigadierCommand command =
                new BrigadierCommand(root.build());

        proxy.getCommandManager().register(
                proxy.getCommandManager()
                        .metaBuilder(getName())
                        .aliases(getAliases().toArray(new String[0]))
                        .build(),
                command
        );

        registered = true;
    }

    private int executeRoot(CommandContext<CommandSource> context) {

        handle(
                context.getSource(),
                getName(),
                new String[0]
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeSub(
            CommandContext<CommandSource> context,
            SubCommand sub,
            String[] args
    ) {

        CommandSource source = context.getSource();

        if (!sub.canExecute(source, args)) {
            sub.noPermission(source, this, getName(), args);
            return Command.SINGLE_SUCCESS;
        }

        sub.execute(source, this, getName(), args);

        return Command.SINGLE_SUCCESS;
    }

    @NotNull
    public abstract String getName();

    @NotNull
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    protected void handle(
            @NotNull CommandSource source,
            @NotNull String label,
            @NotNull String[] args
    ) {
    }
}