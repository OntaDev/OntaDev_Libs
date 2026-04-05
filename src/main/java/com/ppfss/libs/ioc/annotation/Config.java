// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Priority(priority = 103)
public @interface Config {
    int priority() default 0;
}