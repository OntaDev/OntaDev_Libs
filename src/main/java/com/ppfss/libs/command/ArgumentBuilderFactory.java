package com.ppfss.libs.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ArgumentBuilderFactory {

    public static RequiredArgumentBuilder<CommandSource, String> greedyArgs(
            Function<CommandContext<CommandSource>, Integer> executor,
            Function<CommandContext<CommandSource>, List<String>> completer
    ) {

        return RequiredArgumentBuilder
                .<CommandSource, String>argument(
                        "args",
                        StringArgumentType.greedyString()
                )
                .suggests((context, builder) -> {

                    List<String> suggestions =
                            completer.apply(context);

                    for (String suggestion : suggestions) {
                        builder.suggest(suggestion);
                    }

                    return CompletableFuture.completedFuture(
                            builder.build()
                    );
                })
                .executes(executor::apply);
    }

    public static String[] getArgs(
            CommandContext<CommandSource> context
    ) {

        try {

            String raw =
                    context.getArgument("args", String.class);

            return raw.split(" ");

        } catch (Exception ignored) {
            return new String[0];
        }
    }
}