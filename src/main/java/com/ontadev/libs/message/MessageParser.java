// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageParser {

    public static final PlainSerializer PLAIN = MessageParser::extractPlainText;
    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "&(?<legacy>[0-9a-fk-orA-FK-OR])" +
                    "|<(?<slash>/)?(?<tag>#[0-9a-fA-F]{6}|[a-zA-Z_]+)>"
    );

    private static final Map<Character, TextColor> LEGACY_COLORS = buildLegacyColors();
    private static final Map<Character, TextDecoration> LEGACY_DECORATIONS = buildLegacyDecorations();
    private static final Map<String, TextDecoration> TAG_DECORATIONS = buildTagDecorations();

    private MessageParser() {
    }

    @SuppressWarnings("ConstantConditions")
    public static @NotNull Component parse(@NotNull String message) {
        TextComponent.Builder root = Component.text();
        Deque<Style> styleStack = new ArrayDeque<>();
        styleStack.push(Style.empty());

        Matcher matcher = TOKEN_PATTERN.matcher(message);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                root.append(Component.text(message.substring(lastEnd, matcher.start()), styleStack.peek()));
            }
            lastEnd = matcher.end();

            String legacy = matcher.group("legacy");
            if (legacy != null) {
                styleStack.push(applyLegacy(styleStack.pop(), legacy.charAt(0)));
                continue;
            }

            boolean closing = matcher.group("slash") != null;
            if (closing) {
                if (styleStack.size() > 1) {
                    styleStack.pop();
                }
                continue;
            }

            String tag = matcher.group("tag");
            if ("reset".equalsIgnoreCase(tag)) {
                styleStack.push(Style.empty());
                continue;
            }

            Style resolved = applyTag(styleStack.peek(), tag);
            styleStack.push(resolved);
        }

        if (lastEnd < message.length()) {
            root.append(Component.text(message.substring(lastEnd), styleStack.peek()));
        }

        return root.build();
    }

    @FunctionalInterface
    public interface PlainSerializer {
        String serialize(Component component);
    }

    private static String extractPlainText(Component component) {
        StringBuilder builder = new StringBuilder();
        appendPlainText(component, builder);
        return builder.toString();
    }

    private static void appendPlainText(Component component, StringBuilder builder) {
        if (component instanceof TextComponent) {
            builder.append(((TextComponent) component).content());
        }
        for (Component child : component.children()) {
            appendPlainText(child, builder);
        }
    }

    public static String stripColorTags(@NotNull String input) {
        return TOKEN_PATTERN.matcher(input).replaceAll("");
    }

    /** Цвет в ваниле полностью сбрасывает текущее оформление, {@code &r} сбрасывает всё. */
    private static Style applyLegacy(Style current, char code) {
        char lower = Character.toLowerCase(code);
        if (lower == 'r') {
            return Style.empty();
        }

        TextColor color = LEGACY_COLORS.get(lower);
        if (color != null) {
            return Style.style(color);
        }

        TextDecoration decoration = LEGACY_DECORATIONS.get(lower);
        return decoration != null ? current.decorate(decoration) : current;
    }

    /** Именованный/hex-цвет заменяет унаследованный стиль (как новый {@code <tag>}-уровень); неизвестный тег оставляет стиль как есть. */
    private static Style applyTag(Style current, String tag) {
        String lower = tag.toLowerCase(Locale.ROOT);

        if (lower.charAt(0) == '#') {
            TextColor color = TextColor.fromHexString(lower);
            return color != null ? Style.style(color) : current;
        }

        NamedTextColor named = NamedTextColor.NAMES.value(lower);
        if (named != null) {
            return Style.style(named);
        }

        TextDecoration decoration = TAG_DECORATIONS.get(lower);
        return decoration != null ? current.decorate(decoration) : current;
    }

    private static Map<Character, TextColor> buildLegacyColors() {
        Map<Character, TextColor> map = new HashMap<>();
        map.put('0', NamedTextColor.BLACK);
        map.put('1', NamedTextColor.DARK_BLUE);
        map.put('2', NamedTextColor.DARK_GREEN);
        map.put('3', NamedTextColor.DARK_AQUA);
        map.put('4', NamedTextColor.DARK_RED);
        map.put('5', NamedTextColor.DARK_PURPLE);
        map.put('6', NamedTextColor.GOLD);
        map.put('7', NamedTextColor.GRAY);
        map.put('8', NamedTextColor.DARK_GRAY);
        map.put('9', NamedTextColor.BLUE);
        map.put('a', NamedTextColor.GREEN);
        map.put('b', NamedTextColor.AQUA);
        map.put('c', NamedTextColor.RED);
        map.put('d', NamedTextColor.LIGHT_PURPLE);
        map.put('e', NamedTextColor.YELLOW);
        map.put('f', NamedTextColor.WHITE);
        return map;
    }

    private static Map<Character, TextDecoration> buildLegacyDecorations() {
        Map<Character, TextDecoration> map = new HashMap<>();
        map.put('k', TextDecoration.OBFUSCATED);
        map.put('l', TextDecoration.BOLD);
        map.put('m', TextDecoration.STRIKETHROUGH);
        map.put('n', TextDecoration.UNDERLINED);
        map.put('o', TextDecoration.ITALIC);
        return map;
    }

    private static Map<String, TextDecoration> buildTagDecorations() {
        Map<String, TextDecoration> map = new HashMap<>();
        map.put("bold", TextDecoration.BOLD);
        map.put("b", TextDecoration.BOLD);
        map.put("italic", TextDecoration.ITALIC);
        map.put("i", TextDecoration.ITALIC);
        map.put("em", TextDecoration.ITALIC);
        map.put("underlined", TextDecoration.UNDERLINED);
        map.put("u", TextDecoration.UNDERLINED);
        map.put("strikethrough", TextDecoration.STRIKETHROUGH);
        map.put("st", TextDecoration.STRIKETHROUGH);
        map.put("obfuscated", TextDecoration.OBFUSCATED);
        map.put("obf", TextDecoration.OBFUSCATED);
        return map;
    }
}
