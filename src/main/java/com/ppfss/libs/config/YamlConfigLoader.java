// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@SuppressWarnings("unused")
public class YamlConfigLoader {
    private final Gson gson;
    private final Path dataDirectory;
    private final Yaml yaml;
    private final TypeToken<Map<String, Object>> mapToken = new TypeToken<>() {};
    private final Map<String, YamlConfig> cacheConfigs = new ConcurrentHashMap<>();

    public YamlConfigLoader(Path dataDirectory, Map<Class<?>, Object> adapters) {
        this.dataDirectory = dataDirectory;

        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                log.error("Failed to create directory {}", dataDirectory.toAbsolutePath(), e);
                throw new RuntimeException("Could not initialize config directory", e);
            }
        }

        // 1. Настройка SnakeYAML
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);

        // 2. Настройка Gson
        GsonBuilder builder = new GsonBuilder()
                .setPrettyPrinting()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);

        if (adapters != null) {
            adapters.forEach(builder::registerTypeAdapter);
        }

        this.gson = builder.create();
    }

    /**
     * ЛОГИКА СОХРАНЕНИЯ: Class -> Gson -> Snake -> Файл
     */
    public void saveConfig(YamlConfig instance) {
        Path file = instance.getFile();

        try {
            if (!Files.exists(file)) {
                Files.createFile(file);
            }

            // Переводим объект в JsonElement (Gson)
            JsonElement jsonElement = gson.toJsonTree(instance);

            // Превращаем JsonElement в Map (SnakeYAML лучше всего работает с Map)
            Map<String, Object> yamlMap = gson.fromJson(jsonElement, mapToken.getType());

            try (Writer writer = Files.newBufferedWriter(file)) {
                yaml.dump(yamlMap, writer);
            }
        } catch (IOException e) {
            log.error("Error while saving config {}", instance.getFileName(), e);
            throw new RuntimeException("Failed to save " + instance.getFileName(), e);
        }
    }

    /**
     * ЛОГИКА ЗАГРУЗКИ: Файл -> Snake -> Gson -> Class
     */
    public <T extends YamlConfig> T loadFromClass(Class<T> type) {
        try {
            T instance = createEmptyInstance(type);
            String fileName = normalize(instance.getFileName());

            // Кэширование
            if (cacheConfigs.containsKey(fileName)) {
                return type.cast(cacheConfigs.get(fileName));
            }

            Path file = dataDirectory.resolve(fileName);
            instance.setFile(file);
            instance.setConfigLoader(this);

            // Если файла нет — создаем из ресурсов или дефолтный
            if (!Files.exists(file)) {
                handleMissingFile(file, fileName, instance);
                cacheConfigs.put(fileName, instance);
                return instance;
            }

            // 1. Загружаем YAML в Map через SnakeYAML
            Map<String, Object> loadedMap;
            try (Reader reader = Files.newBufferedReader(file)) {
                Object rawYaml = yaml.load(reader);
                // Преобразуем в Map (поддерживает вложенность)
                JsonElement jsonFromYaml = gson.toJsonTree(rawYaml);

                // 2. Превращаем промежуточный Json в целевой Класс через Gson
                T loadedInstance = gson.fromJson(jsonFromYaml, type);

                loadedInstance.setFile(file);
                loadedInstance.setConfigLoader(this);

                // 3. Проверка на наличие новых полей (Defaults)
                if (applyDefaultsFromClass(loadedInstance, rawYaml instanceof Map ? (Map) rawYaml : null)) {
                    saveConfig(loadedInstance);
                    log.info("Config {} updated with new default values.", fileName);
                }

                cacheConfigs.put(fileName, loadedInstance);
                return loadedInstance;
            }

        } catch (Exception e) {
            log.error("Failed to load config class: {}", type.getSimpleName(), e);
            throw new RuntimeException(e);
        }
    }

    private <T extends YamlConfig> void handleMissingFile(Path file, String fileName, T instance) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (in != null) {
                Files.copy(in, file);
            } else {
                saveConfig(instance);
            }
        }
    }

    private <T> T createEmptyInstance(Class<T> type) throws Exception {
        Constructor<T> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private boolean applyDefaultsFromClass(Object instance, Map<String, Object> rawData) {
        if (rawData == null) return false;
        boolean updated = false;

        for (Field field : instance.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;

            if (!rawData.containsKey(field.getName())) {
                updated = true; // Найдено поле в классе, которого нет в YAML
                break;
            }
        }
        return updated;
    }

    public void saveAll() {
        cacheConfigs.values().forEach(this::saveConfig);
    }

    private String normalize(String name) {
        return name.endsWith(".yml") ? name : name + ".yml";
    }
}