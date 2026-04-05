// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.message;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Data;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@SuppressWarnings("unused")
public class Message {
    private static ProxyServer proxy;
    private static Object plugin;

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().strict(false).build();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern LEGACY_TAG_PATTERN = Pattern.compile("</?&([\\p{L}\\p{Nd}_]+)>", Pattern.CASE_INSENSITIVE);

    public static void load(ProxyServer proxyServer, Object pluginInstance) {
        Message.proxy = proxyServer;
        Message.plugin = pluginInstance;
    }

    private final List<String> rawMessage = new CopyOnWriteArrayList<>();

    public Message() {}

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
        proxy.getPlayer(uuid).ifPresent(this::send);
    }

    public void send(Player player) {
        if (player == null) return;
        runSync(() -> {
            for (String line : rawMessage) {
                player.sendMessage(parse(line));
            }
        });
    }

    public void send(CommandSource source) {
        if (source == null) return;
        if (source instanceof Player player) {
            send(player);
            return;
        }
        runSync(() -> {
            for (String line : rawMessage) {
                source.sendMessage(parse(line));
            }
        });
    }

    public void send(Player player, @NotNull Placeholders placeholders) {
        if (player == null) return;
        runSync(() -> {
            for (String line : rawMessage) {
                List<String> expanded = placeholders.apply(line);
                for (String msg : expanded) {
                    player.sendMessage(parse(msg));
                }
            }
        });
    }

    public void send(CommandSource source, @NotNull Placeholders placeholders) {
        if (source == null) return;
        if (source instanceof Player player) {
            send(player, placeholders);
            return;
        }
        runSync(() -> {
            for (String line : rawMessage) {
                List<String> expanded = placeholders.apply(line);
                for (String msg : expanded) {
                    source.sendMessage(parse(msg));
                }
            }
        });
    }

    public void sendActionBar(Audience audience) {
        if (audience == null) return;
        runSync(() -> {
            if (!rawMessage.isEmpty()) {
                audience.sendActionBar(parse(rawMessage.get(0)));
            }
        });
    }

    public void sendActionBar(Audience audience, Placeholders placeholders) {
        if (audience == null) return;

        if (placeholders == null) {
            sendActionBar(audience);
            return;
        }
        runSync(() -> {
            if (!rawMessage.isEmpty()) {
                List<String> expanded = placeholders.apply(rawMessage.get(0));
                if (!expanded.isEmpty()) {
                    audience.sendActionBar(parse(expanded.get(0)));
                }
            }
        });
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

    /**
     * Выполняет задачу синхронно в главном потоке Velocity
     */
    private void runSync(Runnable task) {
        if (proxy == null || plugin == null) {
            task.run();
            return;
        }

        proxy.getScheduler()
                .buildTask(plugin, task)
                .delay(0, TimeUnit.MILLISECONDS)
                .schedule();
    }
}