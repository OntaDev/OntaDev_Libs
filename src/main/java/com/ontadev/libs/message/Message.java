// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.message;

import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.ontadev.libs.message.MessageDispatcher.runOnMain;
import static com.ontadev.libs.message.MessageParser.LEGACY;
import static com.ontadev.libs.message.MessageParser.PLAIN;
import static com.ontadev.libs.message.MessageParser.parse;
import static com.ontadev.libs.message.MessageParser.stripColorTags;

/**
 * Не потокобезопасен для конкурентной мутации (add/clear) во время send.
 * Предполагается паттерн использования: собрали сообщение -> разослали.
 */
@Getter
@SuppressWarnings("unused")
public class Message {

    public static void load(@NotNull Plugin plugin) {
        MessageDispatcher.load(plugin);
    }

    private final List<String> rawMessage = new ArrayList<>();
    private volatile List<Component> parsedCache;

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
        invalidateCache();
    }

    public void addAll(@NotNull List<String> messages) {
        rawMessage.addAll(messages);
        invalidateCache();
    }

    public void add(@NotNull Component component) {
        rawMessage.add(PLAIN.serialize(component));
        invalidateCache();
    }

    public void clear() {
        rawMessage.clear();
        invalidateCache();
    }

    private void invalidateCache() {
        parsedCache = null;
    }

    //
    // Отправка сообщений
    //

    public void send(UUID uuid) {
        runOnMain(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) sendSync(player);
        });
    }

    public void send(Player audience) {
        if (audience == null) return;
        runOnMain(() -> sendSync(audience));
    }

    public void send(CommandSender sender) {
        if (sender == null) return;
        if (sender instanceof Player) {
            send((Player) sender);
            return;
        }
        runOnMain(() -> sendSync(sender));
    }

    public void send(Player player, @NotNull Placeholders placeholders) {
        if (player == null) return;
        runOnMain(() -> sendSync(player, placeholders));
    }

    public void send(CommandSender sender, @NotNull Placeholders placeholders) {
        if (sender == null) return;
        if (sender instanceof Player) {
            send((Player) sender, placeholders);
            return;
        }
        runOnMain(() -> sendSync(sender, placeholders));
    }

    public void sendActionBar(Audience audience) {
        if (audience == null || rawMessage.isEmpty()) return;
        runOnMain(() -> audience.sendActionBar(parse(rawMessage.get(0))));
    }

    public void sendActionBar(Audience audience, Placeholders placeholders) {
        if (audience == null || rawMessage.isEmpty()) return;
        if (placeholders == null) {
            sendActionBar(audience);
            return;
        }
        runOnMain(() -> {
            List<String> expanded = placeholders.apply(rawMessage.get(0));
            if (expanded.isEmpty()) return;
            audience.sendActionBar(parse(expanded.get(0)));
        });
    }

    //
    // Внутренние методы
    //

    private void sendSync(Player player) {
        for (Component component : getParsedCached()) {
            player.sendMessage(component);
        }
    }

    private void sendSync(CommandSender sender) {
        for (Component component : getParsedCached()) {
            sender.sendMessage(LEGACY.serialize(component));
        }
    }

    private void sendSync(Player player, Placeholders placeholders) {
        for (String line : rawMessage) {
            for (String msg : placeholders.apply(line)) {
                player.sendMessage(parse(msg));
            }
        }
    }

    private void sendSync(CommandSender sender, Placeholders placeholders) {
        for (String line : rawMessage) {
            for (String msg : placeholders.apply(line)) {
                sender.sendMessage(LEGACY.serialize(parse(msg)));
            }
        }
    }

    //
    // Геттеры
    //

    /** Первая строка без ЛЮБЫХ плейсхолдеров */
    public String getFirstStringClean() {
        if (rawMessage.isEmpty()) return null;
        return stripColorTags(rawMessage.get(0));
    }

    /** Первая строка, полностью обработанная парсером, без подстановки плейсхолдеров. */
    public String getFirstStringParsed() {
        if (rawMessage.isEmpty()) return null;
        return PLAIN.serialize(parse(rawMessage.get(0)));
    }

    /** Первая строка, полностью обработанная парсером, с подстановкой плейсхолдеров. */
    public String getFirstStringParsed(@NotNull Placeholders placeholders) {
        if (rawMessage.isEmpty()) return null;
        List<String> expanded = placeholders.apply(rawMessage.get(0));
        if (expanded.isEmpty()) return null;
        return PLAIN.serialize(parse(expanded.get(0)));
    }

    /** Первая строка, полностью обработанная парсером, как Component, без плейсхолдеров. */
    public Component getFirstComponentParsed() {
        if (rawMessage.isEmpty()) return null;
        return parse(rawMessage.get(0));
    }

    /** Первая строка, полностью обработанная парсером, как Component, с подстановкой плейсхолдеров. */
    public Component getFirstComponentParsed(@NotNull Placeholders placeholders) {
        if (rawMessage.isEmpty()) return null;
        List<String> expanded = placeholders.apply(rawMessage.get(0));
        if (expanded.isEmpty()) return null;
        return parse(expanded.get(0));
    }

    public List<String> getText() {
        return getText(null);
    }

    public List<String> getText(Placeholders placeholders) {
        List<String> result = new ArrayList<>();
        for (Component component : getComponents(placeholders)) {
            result.add(PLAIN.serialize(component));
        }
        return result;
    }

    public List<Component> getComponents() {
        return getComponents(null);
    }

    public List<Component> getComponents(Placeholders placeholders) {
        if (placeholders == null) {
            return getParsedCached();
        }
        List<Component> result = new ArrayList<>();
        for (String line : rawMessage) {
            for (String msg : placeholders.apply(line)) {
                result.add(parse(msg));
            }
        }
        return result;
    }

    private List<Component> getParsedCached() {
        List<Component> cache = parsedCache;
        if (cache == null) {
            cache = new ArrayList<>(rawMessage.size());
            for (String line : rawMessage) {
                cache.add(parse(line));
            }
            cache = Collections.unmodifiableList(cache);
            parsedCache = cache;
        }
        return cache;
    }

    @Override
    public String toString() {
        return String.join("\n", rawMessage);
    }
}