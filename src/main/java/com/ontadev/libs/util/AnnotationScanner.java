// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public final class AnnotationScanner {
    private AnnotationScanner() {
    }

    public static Set<Class<?>> scanPlugin(JavaPlugin plugin) {
        Set<Class<?>> classes = new HashSet<>();

        ClassLoader pluginLoader = plugin.getClass().getClassLoader();

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()

                .overrideClassLoaders(pluginLoader)

                .ignoreParentClassLoaders()

                .rejectPackages(
                        "org.bukkit",
                        "net.minecraft",
                        "com.mojang",
                        "org.spigotmc",
                        "io.papermc",
                        "io.github.classgraph",
                        "nonapi.io.github.classgraph",
                        "org.slf4j",
                        "ch.qos.logback",
                        "com.google.common",
                        "com.google.gson",
                        "org.apache.commons",
                        "org.jetbrains.annotations",

                        "io.github.classgraph",
                        "nonapi.io.github.classgraph",
                        "org.slf4j",
                        "ch.qos.logback",
                        "com.google.common",
                        "com.google.gson",
                        "com.google.inject",
                        "org.apache.commons",
                        "org.jetbrains.annotations",

                        // Adventure (текстовые компоненты)
                        "net.kyori.adventure",
                        "net.kyori.examination",

                        // Caffeine (кэширование)
                        "com.github.benmanes.caffeine",

                        // Базы данных
                        "com.zaxxer.hikari",
                        "org.h2",
                        "com.mysql",
                        "org.postgresql",

                        "com.mysql.cj",
                        "com.zaxxer.hikari.hibernate",
                        "org.h2.server.web",
                        "org.h2.util",
                        "org.postgresql.osgi",

                        // Дополнительно часто полезно
                        "org.hibernate",
                        "com.mchange.v2.c3p0",
                        "jakarta.servlet",
                        "javax.servlet"
                )

                .scan()) {

            scanResult.getAllClasses().forEach(classInfo -> {
                try {
                    classes.add(classInfo.loadClass());
                } catch (Throwable ignored) {
                }
            });
        }

        return classes;
    }
}
