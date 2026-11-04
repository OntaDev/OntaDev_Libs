// OntaDev_Libs Plugin 
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Регистрирует созданный IoC-компонент как Bukkit-слушатель.
 * Класс должен реализовывать Listener и отдельно регистрироваться как компонент, например через @Component.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoListener {
}