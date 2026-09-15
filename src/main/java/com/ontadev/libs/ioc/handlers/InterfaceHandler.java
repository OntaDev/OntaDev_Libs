// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.handlers;

import com.ontadev.libs.ioc.IoCContainer;

/**
 * IoC сначала проверяет, есть ли реальный класс, implements этот интерфейс
 * (через уже построенную implementationsByInterface карту):
 *  - есть -> onImplementationFound(...) (можно доверить IoC создать как обычный bean, вернув null)
 *  - нет  -> createRealization(...) обязан вернуть готовый инстанс (например Proxy)
 */
public interface InterfaceHandler<I> {

    /** Маркерный интерфейс, который матчим через isAssignableFrom на всех interfaces из classes. */
    Class<I> getMarkerInterface();

    /**
     * Вызывается, если найден конкретный класс, implements iface.
     * Возврат null -> IoC регистрирует implementation и создаёт как обычный bean (стандартный путь).
     * Возврат не-null -> используется как готовый инстанс, обычный create() не вызывается.
     */
    default Object onImplementationFound(IoCContainer container, Class<?> iface, Class<?> implementation) {
        return null;
    }

    /**
     * Вызывается, если реализации не найдено. Обязан вернуть готовый инстанс
     * (proxy и т.п.) — иначе IoC считает интерфейс необработанным и игнорирует его.
     */
    <T> T createRealization(IoCContainer container, Class<T> iface);
}