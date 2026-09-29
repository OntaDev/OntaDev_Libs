// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MessageParserTest {

    @Test
    void plainTextHasNoTags() {
        Component component = MessageParser.parse("Hello world");
        Assertions.assertEquals("Hello world", MessageParser.PLAIN.serialize(component));
    }

    @Test
    void namedColorTagAppliesColor() {
        Component component = MessageParser.parse("<gold>Hello</gold>");
        List<Component> children = component.children();

        Assertions.assertEquals(1, children.size());
        Assertions.assertEquals("Hello", MessageParser.PLAIN.serialize(children.get(0)));
        Assertions.assertEquals(NamedTextColor.GOLD, children.get(0).color());
    }

    @Test
    void hexColorTagAppliesColor() {
        Component component = MessageParser.parse("<#ff0000>Red</#ff0000>");
        Component child = component.children().get(0);

        Assertions.assertEquals(TextColor.fromHexString("#ff0000"), child.color());
    }

    @Test
    void decorationTagAppliesDecoration() {
        Component component = MessageParser.parse("<bold>Bold</bold> normal");
        List<Component> children = component.children();

        Assertions.assertEquals(2, children.size());
        Assertions.assertEquals(TextDecoration.State.TRUE, children.get(0).decoration(TextDecoration.BOLD));
        Assertions.assertEquals(TextDecoration.State.NOT_SET, children.get(1).decoration(TextDecoration.BOLD));
    }

    @Test
    void nestedTagsRestoreOuterStyleAfterClosing() {
        Component component = MessageParser.parse("<gold>a<bold>b</bold>c</gold>");
        List<Component> children = component.children();

        Assertions.assertEquals(3, children.size());

        Assertions.assertEquals(NamedTextColor.GOLD, children.get(0).color());
        Assertions.assertEquals(TextDecoration.State.NOT_SET, children.get(0).decoration(TextDecoration.BOLD));

        Assertions.assertEquals(NamedTextColor.GOLD, children.get(1).color());
        Assertions.assertEquals(TextDecoration.State.TRUE, children.get(1).decoration(TextDecoration.BOLD));

        Assertions.assertEquals(NamedTextColor.GOLD, children.get(2).color());
        Assertions.assertEquals(TextDecoration.State.NOT_SET, children.get(2).decoration(TextDecoration.BOLD));
    }

    @Test
    void legacyColorCodeAppliesColor() {
        Component component = MessageParser.parse("&cRed");
        Assertions.assertEquals(NamedTextColor.RED, component.children().get(0).color());
    }

    @Test
    void legacyResetClearsStyle() {
        Component component = MessageParser.parse("&c&lRed bold&r plain");
        List<Component> children = component.children();

        Assertions.assertEquals(NamedTextColor.RED, children.get(0).color());
        Assertions.assertEquals(TextDecoration.State.TRUE, children.get(0).decoration(TextDecoration.BOLD));

        Assertions.assertNull(children.get(1).color());
        Assertions.assertEquals(TextDecoration.State.NOT_SET, children.get(1).decoration(TextDecoration.BOLD));
    }

    @Test
    void stripColorTagsRemovesBothLegacyAndTagSyntax() {
        String result = MessageParser.stripColorTags("<gold>Hi</gold> &cThere");
        Assertions.assertEquals("Hi There", result);
    }

    @Test
    void unknownTagIsIgnoredButStripped() {
        Component component = MessageParser.parse("<unknown>text</unknown>");
        Assertions.assertEquals("text", MessageParser.PLAIN.serialize(component));
    }
}
