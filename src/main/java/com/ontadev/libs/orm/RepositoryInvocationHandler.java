// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm;

import com.ontadev.libs.orm.annotation.Query;
import com.ontadev.libs.orm.annotation.Modifying;
import com.ontadev.libs.orm.handler.SqlMethodHandler;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

@SuppressWarnings("SqlDialectInspection")

public class RepositoryInvocationHandler implements InvocationHandler {

    private static final String SAVE_METHOD_NAME = "save";

    private final Class<?> entityClass;
    private final SqlMethodHandler methodHandler;

    public RepositoryInvocationHandler(
            Class<?> entityClass,
            SqlMethodHandler methodHandler
    ) {
        this.entityClass = entityClass;
        this.methodHandler = methodHandler;
    }

    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();

        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                declaringClass,
                MethodHandles.lookup()
        );

        return lookup
                .findSpecial(
                        declaringClass,
                        method.getName(),
                        MethodType.methodType(
                                method.getReturnType(),
                                method.getParameterTypes()
                        ),
                        declaringClass
                )
                .bindTo(proxy)
                .invokeWithArguments(args == null ? new Object[0] : args);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            return invokeDefaultMethod(proxy, method, args);
        }

        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }

        if (method.isAnnotationPresent(Query.class)
                || method.isAnnotationPresent(Modifying.class)
                || isSaveMethod(method)) {
            return handleQueryMethod(method, args);
        }

        throw new UnsupportedOperationException(
                "Метод " + method.getName() +
                        " не поддерживается. Используй @Query и @Modifying."
        );
    }

    private boolean isSaveMethod(Method method) {
        return SAVE_METHOD_NAME.equals(method.getName())
                && method.getParameterCount() == 1
                && OrmRepository.class.isAssignableFrom(method.getDeclaringClass());
    }

    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "toString":
                return "OrmRepositoryProxy[" + entityClass.getSimpleName() + "]";
            case "hashCode":
                return System.identityHashCode(proxy);
            case "equals":
                return proxy == (args != null && args.length > 0 ? args[0] : null);
            default:
                return null;
        }
    }

    private Object handleQueryMethod(Method method, Object[] args){
        return methodHandler.handle(method, args);
    }

    private Object handleCustomQuery(String sql, Method method, Object[] args) {
        // TODO: сделать кастом обработку по названию (в будущем)
        return null;
    }
}
