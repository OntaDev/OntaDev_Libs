// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public final class AnnotationScanner {
    private AnnotationScanner() {
    }

    /**
     * Сканирует классы плагина для Velocity
     *
     * @param plugin Экземпляр главного класса плагина
     * @return Множество всех классов плагина
     */
    public static Set<Class<?>> scanPlugin(Object plugin) {
        Set<Class<?>> classes = new HashSet<>();

        ClassLoader pluginLoader = plugin.getClass().getClassLoader();

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()

                .overrideClassLoaders(pluginLoader)

                .ignoreParentClassLoaders()

                .rejectPackages(
                        // Velocity API
                        "com.velocitypowered.api",
                        "com.velocitypowered.proxy",

                        // Общие библиотеки
                        "io.github.classgraph",
                        "nonapi.io.github.classgraph",
                        "org.slf4j",
                        "ch.qos.logback",
                        "com.google.common",
                        "com.google.gson",
                        "com.google.inject",
                        "org.apache.commons",
                        "org.jetbrains.annotations",

                        // Netty (используется Velocity)
                        "io.netty",

                        // Adventure (текстовые компоненты)
                        "net.kyori.adventure",
                        "net.kyori.examination",

                        // Brigadier (команды)
                        "com.mojang.brigadier",

                        // Configurate (конфиги)
                        "org.spongepowered.configurate",

                        // Caffeine (кэширование)
                        "com.github.benmanes.caffeine"
                )

                .scan()) {

            scanResult.getAllClasses().forEach(classInfo -> {
                try {
                    classes.add(classInfo.loadClass());
                } catch (Throwable throwable) {
                    log.warn("Failed to load class {}", classInfo.getName(), throwable);
                }
            });
        }

        return classes;
    }
}