// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.serialization;

import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class GsonAdapterLoader {
    private GsonAdapterLoader() {
    }

    private static final Map<Class<?>, Object> INSTANCE_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    public static void registerAll(GsonBuilder builder, List<Class<?>> adapterClasses) {
        if (builder == null || adapterClasses == null || adapterClasses.isEmpty()) {
            return;
        }

        for (Class<?> adapterClass : adapterClasses) {
            if (adapterClass == null
                    || adapterClass.isInterface()
                    || adapterClass.isAnnotation()
                    || adapterClass.isEnum()) {
                continue;
            }

            GsonAdapter annotation = adapterClass.getAnnotation(GsonAdapter.class);
            if (annotation == null) {
                continue;
            }

            Object adapter = INSTANCE_CACHE.computeIfAbsent(adapterClass, GsonAdapterLoader::createAdapter);
            builder.registerTypeAdapter(annotation.value(), adapter);
        }
    }

    private static Object createAdapter(Class<?> adapterClass) {
        try {
            return adapterClass.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new RuntimeException("Cannot create adapter: " + adapterClass.getName(), ex);
        }
    }
}
