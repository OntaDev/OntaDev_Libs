// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.menu.task;

import com.ontadev.libs.menu.MenuSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@SuppressWarnings("unused")
@Getter
@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class MenuTask {

    /**
     * Задержка перед первым выполнением (в тиках).
     */
    private final long delay;

    /**
     * Период повторения (в тиках).
     * <= 0 означает, что задача выполнится только один раз.
     */
    private final long period;

    /**
     * Действие задачи.
     */
    private final Consumer<MenuSession> consumer;

    public static MenuTask once(long delay, Consumer<MenuSession> consumer) {
        return new MenuTask(delay, -1L, consumer);
    }

    public static MenuTask every(long period, Consumer<MenuSession> consumer) {
        return new MenuTask(period, period, consumer);
    }

    public boolean isRepeating() {
        return period > 0;
    }
}