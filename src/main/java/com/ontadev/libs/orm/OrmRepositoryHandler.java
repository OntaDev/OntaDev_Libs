// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm;

import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.ioc.handlers.InterfaceHandler;
import com.ontadev.libs.orm.database.DatabaseManager;
import com.ontadev.libs.orm.entity.SchemaGenerator;
import com.ontadev.libs.orm.handler.SqlMethodHandler;
import com.ontadev.libs.orm.mapper.EntityMapper;
import com.ontadev.libs.orm.mapper.EntityMetadata;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("rawtypes")
@Slf4j
public class OrmRepositoryHandler implements InterfaceHandler<OrmRepository> {
    private final ConcurrentHashMap<Class<?>, SqlMethodHandler> handlersByInterface = new ConcurrentHashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(8, runnable -> {
        Thread thread = new Thread(runnable, "ontadev-orm-worker");

        thread.setDaemon(true);

        return thread;
    });

    @Override
    public Class<OrmRepository> getMarkerInterface() {
        return OrmRepository.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T createRealization(IoCContainer container, Class<T> iface) {
        DatabaseManager databaseManager = container.get(DatabaseManager.class);
        SchemaGenerator schemaGenerator = container.get(SchemaGenerator.class);
        EntityMapper entityMapper = container.get(EntityMapper.class);

        Class<?> entityType = resolveEntityType(iface);

        schemaGenerator.ensureGenerated(entityType);

        EntityMetadata metadata = entityMapper.metadataOf(entityType);

        SqlMethodHandler methodHandler = handlersByInterface.computeIfAbsent(
                iface,
                key -> new SqlMethodHandler(
                        databaseManager.getJdbi(),
                        executorService,
                        metadata
                )
        );

        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class[]{iface},
                new RepositoryInvocationHandler(entityType, methodHandler)
        );
    }


    private Class<?> resolveEntityType(Class<?> type) {
        for (Type genericInterface : type.getGenericInterfaces()) {
            if (!(genericInterface instanceof ParameterizedType)) {
                continue;
            }

            ParameterizedType parameterizedType = (ParameterizedType) genericInterface;

            if (parameterizedType.getRawType() == OrmRepository.class) {
                Type entityArg = parameterizedType.getActualTypeArguments()[0];

                if (entityArg instanceof Class<?>) {
                    return (Class<?>) entityArg;
                }

                throw new IllegalStateException(
                        "Не удалось определить тип сущности для " + type.getName()
                );
            }
        }

        for (Type genericInterface : type.getGenericInterfaces()) {
            if (!(genericInterface instanceof Class<?>)) {
                continue;
            }

            Class<?> parent = (Class<?>) genericInterface;

            if (OrmRepository.class.isAssignableFrom(parent)) {
                try {
                    return resolveEntityType(parent);
                } catch (IllegalStateException ignored) {
                }
            }
        }

        throw new IllegalStateException(
                "Интерфейс " + type.getName() + " не параметризует OrmRepository<T, ID>"
        );
    }
}