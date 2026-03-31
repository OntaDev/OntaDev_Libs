// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public abstract class AbstractCommand {
    protected final Map<String, SubCommand> subCommands = new HashMap<>();
    @Getter
    private boolean registered = false;

    public AbstractCommand() {
    }

    public void registerSubCommand(SubCommand subCommand) {
        String name = subCommand.getName().toLowerCase();
        subCommands.put(name, subCommand);
    }

    public void register(ProxyServer proxy) {
        if (isRegistered()) {
            throw new IllegalStateException("This command has already been registered");
        }

        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder
                .<CommandSource>literal(getName())
                .executes(this::executeRoot);

        List<String> aliases = getAliases();

        for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
            String subName = entry.getKey();
            SubCommand subCommand = entry.getValue();

            LiteralArgumentBuilder<CommandSource> subBuilder = LiteralArgumentBuilder
                    .<CommandSource>literal(subName)
                    .requires(source -> subCommand.hasPermission(source, this, getName()))
                    .executes(context -> executeSubCommand(context, subCommand, new String[0]));

            RequiredArgumentBuilder<CommandSource, String> argsBuilder = RequiredArgumentBuilder
                    .<CommandSource, String>argument("args", StringArgumentType.greedyString())
                    .suggests(createSuggestionProvider(subCommand))
                    .executes(context -> {
                        String argsString = context.getArgument("args", String.class);
                        String[] args = argsString.split("\\s+");
                        return executeSubCommand(context, subCommand, args);
                    });

            subBuilder.then(argsBuilder);
            builder.then(subBuilder);
        }

        LiteralCommandNode<CommandSource> node = builder.build();
        BrigadierCommand brigadierCommand = new BrigadierCommand(node);

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder(getName())
                        .aliases(aliases.toArray(new String[0]))
                        .build(),
                brigadierCommand
        );

        registered = true;
    }

    private int executeRoot(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        handle(source, getName(), new String[0]);
        return Command.SINGLE_SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private int executeSubCommand(CommandContext<CommandSource> context, SubCommand subCommand, String[] args) {
        CommandSource source = context.getSource();

        if (!subCommand.hasPermission(source, this, getName(), args)) {
            subCommand.noPermission(source, this, getName(), args);
            return Command.SINGLE_SUCCESS;
        }

        subCommand.execute(source, this, getName(), args);
        return Command.SINGLE_SUCCESS;
    }

    private SuggestionProvider<CommandSource> createSuggestionProvider(SubCommand subCommand) {
        return (context, builder) -> {
            CommandSource source = context.getSource();
            String input = builder.getRemaining();
            String[] args = input.isEmpty() ? new String[0] : input.split("\\s+");

            List<String> suggestions = subCommand.complete(source, args);

            for (String suggestion : suggestions) {
                builder.suggest(suggestion);
            }

            return builder.buildFuture();
        };
    }

    public abstract @NotNull String getName();

    public @NotNull List<String> getAliases() {
        return Collections.emptyList();
    }

    protected void handle(CommandSource source, String commandLabel, String[] args) {
    }

    public List<String> complete(CommandSource source, String label, String... args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();

            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                SubCommand subCommand = entry.getValue();
                String name = entry.getKey();

                if (!subCommand.hasPermission(source, this, label)) {
                    continue;
                }
                result.add(name);
            }
            return result;

        } else if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                return subCommand.complete(source, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> strings, String... args) {
        if (strings == null || strings.isEmpty()) return new ArrayList<>();
        String lastArg = args[args.length - 1].toLowerCase().trim();
        List<String> filtered = new ArrayList<>();
        for (String string : strings) {
            if (string.toLowerCase().startsWith(lastArg)) {
                filtered.add(string);
            }
        }
        return filtered;
    }
}