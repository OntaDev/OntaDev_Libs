// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageParser {

    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder().strict(false).build();
    public static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private static final Pattern LEGACY_TAG_PATTERN =
            Pattern.compile("</?&([\\p{L}\\p{Nd}_]+)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINIMESSAGE_TAG_PATTERN =
            Pattern.compile("</?[a-zA-Z0-9_:#]+(:[^>]*)?>");
    private static final Pattern LEGACY_COLOR_PATTERN =
            Pattern.compile("&[0-9a-fk-orA-FK-OR]");

    private MessageParser() {
    }

    public static @NotNull Component parse(@NotNull String message) {
        String normalized = normalizeLegacyTags(message);
        Component legacy = LEGACY.deserialize(normalized);
        return MINI_MESSAGE.deserialize(normalized).mergeStyle(legacy);
    }

    public static String stripColorTags(@NotNull String input) {
        String noLegacy = LEGACY_COLOR_PATTERN.matcher(input).replaceAll("");
        return MINIMESSAGE_TAG_PATTERN.matcher(noLegacy).replaceAll("");
    }

    private static String normalizeLegacyTags(@NotNull String input) {
        Matcher matcher = LEGACY_TAG_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String tag = matcher.group(1).toLowerCase(Locale.ROOT);
            String replacement = matcher.group().startsWith("</") ? "</" + tag + ">" : "<" + tag + ">";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}