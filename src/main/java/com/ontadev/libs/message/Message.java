// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT

package com.ontadev.libs.message;

import lombok.Data;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@SuppressWarnings("unused")
public class Message {
    private static Plugin plugin;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().strict(false).build();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern LEGACY_TAG_PATTERN = Pattern.compile("</?&([\\p{L}\\p{Nd}_]+)>", Pattern.CASE_INSENSITIVE);

    public static void load(Plugin plugin) {
        Message.plugin = plugin;
    }

    private final List<String> rawMessage = new CopyOnWriteArrayList<>();

    public Message() {
    }

    public Message(@NotNull String... messages) {
        Collections.addAll(rawMessage, messages);
    }

    public Message(@NotNull List<String> messages) {
        rawMessage.addAll(messages);
    }

    public Message(@NotNull Component... components) {
        for (Component component : components) {
            rawMessage.add(PLAIN.serialize(component));
        }
    }

    public void add(@NotNull String message) {
        rawMessage.add(message);
    }

    public void addAll(@NotNull List<String> messages) {
        rawMessage.addAll(messages);
    }

    public void add(@NotNull Component component) {
        rawMessage.add(PLAIN.serialize(component));
    }

    public void clear() {
        rawMessage.clear();
    }

    public void send(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) send(player);
    }

    public void send(Player audience) {
        if (audience == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String line : rawMessage) {
                    audience.sendMessage(parse(line));
                }
            }
        }.runTask(plugin);
    }

    public void send(CommandSender sender) {
        if (sender == null) return;
        if (sender instanceof Player) {
            send((Player) sender);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String line : rawMessage) {
                    Component component = parse(line);
                    String legacyMessage = LEGACY.serialize(component);
                    sender.sendMessage(legacyMessage);
                }
            }
        }.runTask(plugin);
    }

    public void send(Player player, @NotNull Placeholders placeholders) {
        if (player == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String line : rawMessage) {
                    List<String> expanded = placeholders.apply(line);
                    for (String msg : expanded) {
                        player.sendMessage(parse(msg));
                    }
                }
            }
        }.runTask(plugin);
    }

    public void send(CommandSender player, @NotNull Placeholders placeholders) {
        if (player == null) return;
        if (player instanceof Player) {
            send((Player) player, placeholders);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String line : rawMessage) {
                    List<String> expanded = placeholders.apply(line);
                    for (String msg : expanded) {
                        Component component = parse(msg);
                        String legacyMessage = LEGACY.serialize(component);
                        player.sendMessage(legacyMessage);
                    }
                }
            }
        }.runTask(plugin);
    }

    public void sendActionBar(Audience player) {
        if (player == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendActionBar(parse(rawMessage.get(0)));
            }
        }.runTask(plugin);
    }

    public void sendActionBar(Audience player, Placeholders placeholders) {
        if (player == null) return;

        if (placeholders == null) {
            sendActionBar(player);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                List<String> expanded = placeholders.apply(rawMessage.get(0));
                player.sendActionBar(parse(expanded.get(0)));
            }
        }.runTask(plugin);
    }


    public List<String> getText() {
        return getText(null);
    }

    public List<String> getText(Placeholders placeholders) {
        List<String> result = new ArrayList<>();
        for (String line : rawMessage) {
            if (placeholders != null) {
                for (String msg : placeholders.apply(line)) {
                    result.add(PLAIN.serialize(parse(msg)));
                }
            } else {
                result.add(PLAIN.serialize(parse(line)));
            }
        }
        return result;
    }

    public List<Component> getComponents() {
        return getComponents(null);
    }

    public List<Component> getComponents(Placeholders placeholders) {
        List<Component> result = new ArrayList<>();
        for (String line : rawMessage) {
            if (placeholders != null) {
                for (String msg : placeholders.apply(line)) {
                    result.add(parse(msg));
                }
            } else {
                result.add(parse(line));
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return String.join("\n", rawMessage);
    }


    private @NotNull Component parse(@NotNull String message) {
        String normalized = normalizeLegacyTags(message);

        Component legacy = LEGACY.deserialize(normalized);

        return MINI_MESSAGE.deserialize(normalized).mergeStyle(legacy);
    }

    @SuppressWarnings("StringBufferMayBeStringBuilder")
    private String normalizeLegacyTags(@NotNull String input) {
        Matcher matcher = LEGACY_TAG_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String tag = matcher.group(1).toLowerCase(Locale.ROOT);
            String replacement = matcher.group().startsWith("</") ? "</" + tag + ">" : "<" + tag + ">";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

}
