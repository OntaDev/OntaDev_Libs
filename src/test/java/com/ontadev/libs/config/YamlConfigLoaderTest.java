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
    void shouldReturnCachedInstanceOnSecondLoad(@TempDir Path dir) {
        YamlConfigLoader loader = new YamlConfigLoader(dir, null);

        SampleConfig first = loader.loadFromClass(SampleConfig.class);
        SampleConfig second = loader.loadFromClass(SampleConfig.class);

        Assertions.assertSame(first, second);
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
