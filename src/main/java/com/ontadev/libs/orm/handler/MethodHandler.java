// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.handler;

import java.lang.reflect.Method;

public interface MethodHandler {
    Object handle(Method method, Object[] args);
}
