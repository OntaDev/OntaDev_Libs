// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT

package com.ontadev.libs.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public abstract class AbstractCommand extends Command {
    protected final Map<String, SubCommand> subCommands = new HashMap<>();

    public AbstractCommand() {
        super("temp");
    }


    public void registerSubCommand(SubCommand subCommand) {
        String name = subCommand.getName().toLowerCase();

        subCommands.put(name, subCommand);
    }

    public void register(Plugin plugin) {
        if (isRegistered()){
            throw new IllegalStateException("This command has already been registered");
        }

        this.setName(getName());

        List<String> aliases = new ArrayList<>();

        Bukkit.getCommandMap().register(plugin.getName(), this);
    }


    public abstract @NotNull String getName();

    public @NotNull List<String> getAliases() {return Collections.emptyList();}

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length > 0 && !subCommands.isEmpty()) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

                if (!subCommand.hasPermission(sender, this, "", subArgs)) {
                    subCommand.noPermission(sender, this, commandLabel, subArgs);
                    return true;
                }

                subCommand.execute(sender, this, commandLabel, subArgs);
                return true;
            }
        }
        handle(sender, this, commandLabel, args);
        return true;
    }

    protected void handle(CommandSender sender, Command command, String commandLabel, String[] args) {}

    private List<String> filter(List<String> strings, String... args) {
        if (strings == null || strings.isEmpty()) return new ArrayList<>();
        String lastArg = args[args.length - 1].toLowerCase().trim();
        List<String> filtered = new ArrayList<>();
        for (String string : strings) {
            if (string.toLowerCase().startsWith(lastArg.toLowerCase())) filtered.add(string);
        }
        return filtered;
    }

    public List<String> complete(CommandSender sender, String label, String... args) {

        if (args.length == 1) {
            List<String> result = new ArrayList<>();

            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                SubCommand subCommand = entry.getValue();
                String name = entry.getKey();
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

                if (!subCommand.hasPermission(sender, this, label, subArgs)) {
                    continue;
                }
                result.add(name);
            }
            return result;

        } else if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                return subCommand.complete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return Collections.emptyList();
    }


    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return filter(complete(sender, alias, args), args);
    }
}
