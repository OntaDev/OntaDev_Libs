// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ppfss.libs.ioc.IoCContainer;
import com.ppfss.libs.serialization.GsonAdapter;
import com.ppfss.libs.serialization.GsonAdapterLoader;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SuppressWarnings("unused")
public class YamlConfigLoader {
    private final AtomicReference<Gson> gson;
    private final Path dataDirectory;
    private final Yaml yaml;
    private final TypeToken<Map<String, Object>> mapToken = new TypeToken<>() {};
    private final Map<String, YamlConfig> cacheConfigs = new ConcurrentHashMap<>();

    public YamlConfigLoader(Path dataDirectory, IoCContainer container) {
        this.dataDirectory = dataDirectory;

        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                log.error("Failed to create directory {}", dataDirectory.toAbsolutePath(), e);
                throw new RuntimeException("Failed to create directory " + dataDirectory.toAbsolutePath(), e);
            }
        }

        // Инициализация SnakeYAML
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);

        GsonBuilder builder = new GsonBuilder()
                .setPrettyPrinting()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);

        container.getAllClassesWithAnnotation(GsonAdapter.class).forEach(adapterClass -> {
            Object adapter = container.get(adapterClass);
            GsonAdapter annotation = adapterClass.getAnnotation(GsonAdapter.class);
            builder.registerTypeAdapter(annotation.value(), adapter);
        });

        this.gson = new AtomicReference<>(builder.create());
    }

    private boolean applyDefaultsFromClass(Object instance, Map<String, Object> config) {
        boolean updated = false;
        Class<?> clazz = instance.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
            field.setAccessible(true);
            String path = field.getName();

            if (!config.containsKey(path)) {
                try {
                    Object value = field.get(instance);
                    if (value != null) {
                        config.put(path, value);
                        updated = true;
                    }
                } catch (IllegalAccessException e) {
                    log.error("Failed to read field {} in {}", path, clazz.getSimpleName(), e);
                }
            }
        }

        return updated;
    }

    private Map<String, Object> flattenMap(Object obj) {
        if (obj instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            ((Map<?, ?>) obj).forEach((key, value) -> {
                if (value instanceof Map) {
                    result.put(String.valueOf(key), flattenMap(value));
                } else {
                    result.put(String.valueOf(key), value);
                }
            });
            return result;
        }
        return new HashMap<>();
    }

    public void saveAll() {
        cacheConfigs.values().forEach(this::saveConfig);
    }

    public void saveConfig(YamlConfig instance) {
        Path file = instance.getFile();

        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
                log.info("Created config file {}", file.getFileName());
            } catch (IOException e) {
                throw new RuntimeException("Can't create file: " + file.getFileName(), e);
            }
        }

        String json = getGson().toJson(instance);
        Map<String, Object> map = getGson().fromJson(json, mapToken.getType());

        try (Writer writer = Files.newBufferedWriter(file)) {
            yaml.dump(map, writer);
        } catch (IOException e) {
            log.error("Error while saving config {}", file.getFileName(), e);
            throw new RuntimeException("Error while saving config " + file.getFileName(), e);
        }
    }

    public <T extends YamlConfig> T loadFromClass(Class<T> type) {
        Constructor<T> constructor = findEmptyConstructor(type);

        if (constructor == null) {
            log.error("Can't find empty constructor for {}", type.getName());
            throw new RuntimeException("Can't find empty constructor for " + type.getName());
        }

        try {
            constructor.setAccessible(true);
            T instance = constructor.newInstance();

            String fileName = normalize(instance.getFileName());

            YamlConfig cached = cacheConfigs.get(fileName);
            if (type.isInstance(cached)) {
                return type.cast(cached);
            }

            Path file = dataDirectory.resolve(fileName);

            if (!Files.exists(file)) {
                // Попытка скопировать из ресурсов (если есть)
                try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
                    if (in == null) {
                        instance.setFile(file);
                        instance.setConfigLoader(this);
                        saveConfig(instance);

                        cacheConfigs.put(fileName, instance);
                        return instance;
                    }

                    Files.copy(in, file);
                } catch (IOException e) {
                    log.error("Can't copy {}", fileName, e);
                    throw new RuntimeException("Can't copy " + fileName, e);
                }
            }

            Map<String, Object> data;
            try (Reader reader = Files.newBufferedReader(file)) {
                Object loaded = yaml.load(reader);
                data = flattenMap(loaded);
            }

            String json = getGson().toJson(data);
            T loaded = getGson().fromJson(json, type);

            loaded.setFile(file);
            loaded.setConfigLoader(this);

            boolean updated = applyDefaultsFromClass(loaded, data);
            if (updated) {
                try (Writer writer = Files.newBufferedWriter(file)) {
                    yaml.dump(data, writer);
                }
                log.info("Updated config with new defaults: {}", fileName);
            }

            cacheConfigs.put(fileName, loaded);
            return loaded;

        } catch (Exception e) {
            log.error("Can't load {}", type.getName(), e);
            throw new RuntimeException("Can't load " + type.getName(), e);
        }
    }

    private <T> Constructor<T> findEmptyConstructor(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalize(String name) {
        return name.endsWith(".yml") ? name : name + ".yml";
    }

    public void reloadAdapters(List<Class<?>> adapterClasses) {
        gson.set(createGson(adapterClasses));
    }

    private static Gson createGson(List<Class<?>> adapterClasses) {
        GsonBuilder builder = new GsonBuilder()
                .setPrettyPrinting()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);
        GsonAdapterLoader.registerAll(builder, adapterClasses);
        return builder.create();
    }

    private Gson getGson() {
        return gson.get();
    }
}