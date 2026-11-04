// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.config;

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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Загружает и кеширует YAML-конфиги, сохраняя комментарии при записи.
 * Новые файлы копируются из ресурсов либо генерируются из значений класса.
 */
@Slf4j
@SuppressWarnings("unused")
public class YamlConfigLoader {
    private final Gson gson;
    private final Path dataDirectory;
    private final Yaml yaml;
    private final CommentPreservingYaml preservingYaml = new CommentPreservingYaml();
    private final TypeToken<Map<String, Object>> mapToken = new TypeToken<>() {};
    private final Map<String, YamlConfig> cacheConfigs = new ConcurrentHashMap<>();

    /**
     * Создаёт загрузчик и при необходимости каталог для конфигов.
     *
     * @param dataDirectory каталог YAML-файлов
     * @param adapters адаптеры Gson по типам; null означает отсутствие адаптеров
     * @throws RuntimeException если создать каталог не удалось
     */
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

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);

        GsonBuilder builder = new GsonBuilder()
                .setPrettyPrinting()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);

        if (adapters != null) {
            adapters.forEach(builder::registerTypeAdapter);
        }

        this.gson = builder.create();
    }

    /**
     * Сохраняет значения привязанного к файлу конфига.
     * Существующие комментарии и неизвестные ключи сохраняются;
     * аннотации {@link Comment} повторно не применяются.
     *
     * @param instance конфиг с назначенным путём файла
     * @throws RuntimeException если записать файл не удалось
     */
    public void saveConfig(YamlConfig instance) {
        saveConfig(instance, false);
    }

    private void saveConfig(YamlConfig instance, boolean generateComments) {
        Path file = instance.getFile();

        try {
            JsonElement jsonElement = gson.toJsonTree(instance);

            Map<String, Object> yamlMap = gson.fromJson(jsonElement, mapToken.getType());
            preservingYaml.save(file, yamlMap, generateComments ? instance : null);
        } catch (IOException e) {
            log.error("Error while saving config {}", instance.getFileName(), e);
            throw new RuntimeException("Failed to save " + instance.getFileName(), e);
        }
    }

    /**
     * Перечитывает файл, обновляя существующий экземпляр конфига.
     * Ошибки чтения и десериализации не изменяют его текущие значения.
     *
     * @param instance конфиг, который нужно обновить
     * @param <T> тип конфига
     * @throws IllegalStateException если конфиг не привязан к файлу
     * @throws RuntimeException если чтение или десериализация завершились ошибкой
     */
    public <T extends YamlConfig> void reloadConfig(T instance) {
        Path file = instance.getFile();
        if (file == null) {
            throw new IllegalStateException("Config is not attached to a file: " + instance.getFileName());
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            Object rawYaml = yaml.load(reader);
            JsonElement jsonFromYaml = gson.toJsonTree(rawYaml);

            @SuppressWarnings("unchecked")
            T loaded = (T) gson.fromJson(jsonFromYaml, instance.getClass());
            loaded.setFile(file);
            loaded.setConfigLoader(this);
            instance.applyFrom(loaded);
        } catch (Exception e) {
            log.error("Failed to reload config {}", instance.getFileName(), e);
            throw new RuntimeException("Failed to reload " + instance.getFileName(), e);
        }
    }

    /**
     * Загружает конфиг или возвращает ранее кешированный экземпляр по имени файла.
     * Класс должен иметь конструктор без аргументов.
     *
     * <p>Если файла нет, ресурс с тем же именем копируется без изменений.
     * Если ресурса нет, файл генерируется из значений класса с комментариями
     * {@link Comment}. Для существующих файлов аннотация не применяется.</p>
     *
     * @param type класс конфига
     * @param <T> тип конфига
     * @return привязанный к файлу экземпляр
     * @throws RuntimeException если создать или загрузить конфиг не удалось
     */
    public <T extends YamlConfig> T loadFromClass(Class<T> type) {
        try {
            T instance = createEmptyInstance(type);
            String fileName = normalize(instance.getFileName());

            if (cacheConfigs.containsKey(fileName)) {
                return type.cast(cacheConfigs.get(fileName));
            }

            Path file = dataDirectory.resolve(fileName);
            instance.setFile(file);
            instance.setConfigLoader(this);

            if (!Files.exists(file)) {
                handleMissingFile(file, fileName, instance);
                cacheConfigs.put(fileName, instance);
                return instance;
            }

            try (Reader reader = Files.newBufferedReader(file)) {
                Object rawYaml = yaml.load(reader);
                JsonElement jsonFromYaml = gson.toJsonTree(rawYaml);

                T loadedInstance = gson.fromJson(jsonFromYaml, type);

                loadedInstance.setFile(file);
                loadedInstance.setConfigLoader(this);

                Map<?, ?> rawData = rawYaml instanceof Map<?, ?> ? (Map<?, ?>) rawYaml : null;
                if (applyDefaultsFromClass(loadedInstance, rawData)) {
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
                saveConfig(instance, true);
            }
        }
    }

    private <T> T createEmptyInstance(Class<T> type) throws Exception {
        Constructor<T> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private boolean applyDefaultsFromClass(Object instance, Map<?, ?> rawData) {
        if (rawData == null) return false;
        boolean updated = false;

        for (Field field : instance.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;

            if (!rawData.containsKey(field.getName())) {
                updated = true;
                break;
            }
        }
        return updated;
    }

    /**
     * Сохраняет все кешированные конфиги.
     *
     * @throws RuntimeException при первой ошибке сохранения;
     *         оставшиеся конфиги могут остаться несохранёнными
     */
    public void saveAll() {
        cacheConfigs.values().forEach(this::saveConfig);
    }

    private String normalize(String name) {
        return name.endsWith(".yml") ? name : name + ".yml";
    }
}
