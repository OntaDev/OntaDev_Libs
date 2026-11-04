// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.config;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

public class YamlConfigLoaderTest {

    @Getter
    @Setter
    public static class SampleConfig extends YamlConfig {
        private String name = "default";
        private int amount = 10;

        @Override
        public String getFileName() {
            return "sample";
        }
    }

    @Getter
    @Setter
    public static class ExtendedSampleConfig extends YamlConfig {
        private String name = "default";
        private int amount = 10;
        private boolean enabled = true;

        @Override
        public String getFileName() {
            return "sample";
        }
    }

    public static class CommentedConfig extends YamlConfig {
        @Comment("First line\nВторая строка")
        @com.google.gson.annotations.SerializedName("custom-name")
        private String name = "default";

        @Comment("Section")
        private Nested nested = new Nested();

        @Override
        public String getFileName() { return "commented"; }
    }

    public static class Nested {
        @Comment("Nested value")
        private int count = 5;
    }

    public static class ResourceConfig extends YamlConfig {
        @Comment("Must not appear")
        private String name = "class-default";

        @Override
        public String getFileName() { return "comment-resource"; }
    }

    @Test
    void shouldGenerateCommentsAndPreserveUserEdits(@TempDir Path dir) throws Exception {
        CommentedConfig config = new YamlConfigLoader(dir, null).loadFromClass(CommentedConfig.class);
        Path file = dir.resolve("commented.yml");
        String generated = Files.readString(file);
        Assertions.assertTrue(generated.contains("# First line\n# Вторая строка\ncustom-name: default"));
        Assertions.assertTrue(generated.contains("# Section\nnested:"));
        Assertions.assertTrue(generated.contains("  # Nested value\n  count:"));
        config.save();
        Assertions.assertEquals(generated, Files.readString(file));
        String edited = generated.replace("# First line", "# User comment");
        Files.writeString(file, edited);
        config.save();
        Assertions.assertEquals(edited, Files.readString(file));
    }

    @Test
    void shouldCopyResourceWithoutClassComments(@TempDir Path dir) throws Exception {
        ResourceConfig config = new YamlConfigLoader(dir, null).loadFromClass(ResourceConfig.class);
        String expected;
        try (java.io.InputStream in = getClass().getResourceAsStream("/comment-resource.yml")) {
            expected = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        Path file = dir.resolve("comment-resource.yml");
        Assertions.assertEquals(expected, Files.readString(file));
        config.save();
        Assertions.assertFalse(Files.readString(file).contains("Must not appear"));
        new YamlConfigLoader(dir, null).loadFromClass(ResourceConfig.class).save();
        Assertions.assertFalse(Files.readString(file).contains("Must not appear"));
    }

    @Test
    void shouldCreateFileWithDefaultsWhenMissing(@TempDir Path dir) {
        YamlConfigLoader loader = new YamlConfigLoader(dir, null);

        SampleConfig config = loader.loadFromClass(SampleConfig.class);

        Assertions.assertTrue(Files.exists(dir.resolve("sample.yml")));
        Assertions.assertEquals("default", config.getName());
        Assertions.assertEquals(10, config.getAmount());
    }

    @Test
    void shouldSaveAndReloadModifiedValues(@TempDir Path dir) {
        YamlConfigLoader loader = new YamlConfigLoader(dir, null);

        SampleConfig config = loader.loadFromClass(SampleConfig.class);
        config.setName("custom");
        config.setAmount(42);
        config.save();

        YamlConfigLoader freshLoader = new YamlConfigLoader(dir, null);
        SampleConfig reloaded = freshLoader.loadFromClass(SampleConfig.class);

        Assertions.assertEquals("custom", reloaded.getName());
        Assertions.assertEquals(42, reloaded.getAmount());
    }

    @Test
    void shouldPreserveCommentsUnknownKeysAndFormattingIntent(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("sample.yml");
        Files.writeString(file, "# Main comment\nname: old # inline comment\namount: 10\nunknown: keep-me\n");
        YamlConfigLoader loader = new YamlConfigLoader(dir, null);
        SampleConfig config = loader.loadFromClass(SampleConfig.class);

        config.setName("new-value");
        config.setAmount(25);
        config.save();

        String saved = Files.readString(file);
        Assertions.assertTrue(saved.contains("# Main comment"));
        Assertions.assertTrue(saved.contains("# inline comment"));
        Assertions.assertTrue(saved.contains("unknown: keep-me"));
        Assertions.assertTrue(saved.contains("name: new-value"));
        Assertions.assertTrue(saved.contains("amount: 25"));
    }

    @Test
    void shouldReturnCachedInstanceOnSecondLoad(@TempDir Path dir) {
        YamlConfigLoader loader = new YamlConfigLoader(dir, null);

        SampleConfig first = loader.loadFromClass(SampleConfig.class);
        SampleConfig second = loader.loadFromClass(SampleConfig.class);

        Assertions.assertSame(first, second);
    }

    @Test
    void shouldReloadFileIntoSameInstance(@TempDir Path dir) throws Exception {
        YamlConfigLoader loader = new YamlConfigLoader(dir, null);
        SampleConfig config = loader.loadFromClass(SampleConfig.class);

        Files.writeString(dir.resolve("sample.yml"), "name: reloaded\namount: 73\n");
        config.reload();

        Assertions.assertEquals("reloaded", config.getName());
        Assertions.assertEquals(73, config.getAmount());
        Assertions.assertSame(config, loader.loadFromClass(SampleConfig.class));
    }

    @Test
    void shouldKeepCurrentValuesWhenReloadFails(@TempDir Path dir) throws Exception {
        YamlConfigLoader loader = new YamlConfigLoader(dir, null);
        SampleConfig config = loader.loadFromClass(SampleConfig.class);
        config.setName("still-current");

        Files.writeString(dir.resolve("sample.yml"), "broken: [yaml\n");

        Assertions.assertThrows(RuntimeException.class, config::reload);
        Assertions.assertEquals("still-current", config.getName());
    }

    @Test
    void shouldUpdateFileWithNewDefaultsWhenClassGainsFields(@TempDir Path dir) throws Exception {
        YamlConfigLoader loader = new YamlConfigLoader(dir, null);
        loader.loadFromClass(SampleConfig.class);

        String beforeUpdate = Files.readString(dir.resolve("sample.yml"));
        Assertions.assertFalse(beforeUpdate.contains("enabled"));

        YamlConfigLoader freshLoader = new YamlConfigLoader(dir, null);
        ExtendedSampleConfig updated = freshLoader.loadFromClass(ExtendedSampleConfig.class);

        Assertions.assertTrue(updated.isEnabled());
        String afterUpdate = Files.readString(dir.resolve("sample.yml"));
        Assertions.assertTrue(afterUpdate.contains("enabled"));
    }

    @Test
    void shouldCreateDataDirectoryWhenMissing(@TempDir Path tempDir) {
        Path nested = tempDir.resolve("nested/config-dir");

        new YamlConfigLoader(nested, null);

        Assertions.assertTrue(Files.isDirectory(nested));
    }
}
