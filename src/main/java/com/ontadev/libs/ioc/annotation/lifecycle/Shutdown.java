// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.annotation.lifecycle;

import java.lang.annotation.*;

/**
 * Вызывает метод IoC-компонента при отключении плагина.
 * Метод должен быть без аргументов; допускается private-доступ.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Shutdown {
}