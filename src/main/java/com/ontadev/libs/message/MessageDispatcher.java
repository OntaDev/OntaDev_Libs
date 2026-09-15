// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.message;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Статический диспетчер выполнения задач в основном потоке Bukkit.
 * Инициализируется один раз через load(plugin), обычно вместе с Message.load().
 */
public final class MessageDispatcher {

    private static Plugin plugin;

    private MessageDispatcher() {
    }

    public static void load(@NotNull Plugin plugin) {
        MessageDispatcher.plugin = plugin;
    }

    public static void runOnMain(@NotNull Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}