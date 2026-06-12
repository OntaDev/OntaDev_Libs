// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc;

import com.ontadev.libs.ioc.annotation.Inject;

import java.lang.reflect.Constructor;

public class ConstructorResolver {

    public static Constructor<?> resolve(Class<?> clazz) throws NoSuchMethodException {

        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                return constructor;
            }
        }

        Constructor<?>[] constructors = clazz.getConstructors();

        if (constructors.length == 1) {
            return constructors[0];
        }

        return clazz.getDeclaredConstructor();
    }

}