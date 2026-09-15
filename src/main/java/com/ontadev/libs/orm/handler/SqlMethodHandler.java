// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.handler;

import com.ontadev.libs.exception.AnnotationNotFound;
import com.ontadev.libs.exception.InvalidEntityException;
import com.ontadev.libs.exception.InvalidRepositoryMethodException;
import com.ontadev.libs.exception.NoResultException;
import com.ontadev.libs.orm.OrmRepository;
import com.ontadev.libs.orm.annotation.Async;
import com.ontadev.libs.orm.annotation.Query;
import com.ontadev.libs.orm.annotation.Modifying;
import com.ontadev.libs.orm.dto.SaveContext;
import com.ontadev.libs.orm.mapper.EntityMetadata;
import com.ontadev.libs.orm.mapper.FieldMeta;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
public class SqlMethodHandler implements MethodHandler {
    private static final String SAVE_METHOD_NAME = "save";

    private final Jdbi jdbi;
    private final ExecutorService executorService;
    private final EntityMetadata metadata;

    private final ConcurrentHashMap<Method, Function<Object[], Object>> cache = new ConcurrentHashMap<>();

    @Override
    public Object handle(Method method, Object[] args) {
        if (isSaveMethod(method)) {
            return cache.computeIfAbsent(method, this::compileSave).apply(args);
        }
        return cache.computeIfAbsent(method, this::compile).apply(args);
    }

    private boolean isSaveMethod(Method method) {
        return SAVE_METHOD_NAME.equals(method.getName())
                && method.getParameterCount() == 1
                && OrmRepository.class.isAssignableFrom(method.getDeclaringClass());
    }

    private Function<Object[], Object> compile(Method method) {
        ReturnTypeInfo type = parse(method);

        if (method.isAnnotationPresent(Async.class)
                && !type.hasWrapper(WrapperType.ASYNC)) {
            throw new InvalidRepositoryMethodException(method.getName()
                    + " должен возвращать CompletableFuture");
        }

        String sql;
        if (method.isAnnotationPresent(Query.class)) {
            sql = method.getAnnotation(Query.class).value();
        } else if (method.isAnnotationPresent(Modifying.class)) {
            sql = method.getAnnotation(Modifying.class).value();
        } else {
            throw new AnnotationNotFound();
        }

        ParamBinding[] bindings = compileBindings(method, sql);

        if (method.isAnnotationPresent(Query.class)) {
            Query annotation = method.getAnnotation(Query.class);
            return args -> handleQuery(annotation, type, bindings, args);
        }

        Modifying annotation = method.getAnnotation(Modifying.class);
        return args -> handleUpdate(annotation, type, bindings, args);
    }

    private ParamBinding[] compileBindings(Method method, String sql) {
        Parameter[] parameters = method.getParameters();
        ParamBinding[] bindings = new ParamBinding[parameters.length];

        boolean sqlUsesPositional = containsPositionalPlaceholder(sql);

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Bind annotation = parameter.getAnnotation(Bind.class);

            if (annotation != null) {
                bindings[i] = ParamBinding.named(annotation.value());
                continue;
            }

            if (sqlUsesPositional) {
                bindings[i] = ParamBinding.positional(i);
                continue;
            }

            if (parameter.isNamePresent()) {
                bindings[i] = ParamBinding.named(parameter.getName());
                continue;
            }

            throw new InvalidRepositoryMethodException(
                    "Параметр #" + i + " метода "
                            + method.getDeclaringClass().getSimpleName() + "." + method.getName()
                            + " не может быть привязан по имени: SQL использует именованные "
                            + "плейсхолдеры (:name), но имя параметра недоступно. "
                            + "Добавьте компиляторный флаг -parameters либо аннотацию @Bind."
            );
        }

        return bindings;
    }

    private Function<Object[], Object> compileSave(Method method) {

        Field idField = metadata.idField();

        if (idField.getType().isPrimitive()) {
            throw new InvalidEntityException(
                    "Поле ID " + idField.getName() + " в " +  metadata.entityClass().getSimpleName()
                            + " должно быть wrapper-типом (Long/Integer/...), не примитивом — "
                            + "иначе save() не может отличить \"ID не задан\" от \"ID равен нулю\""
            );
        }
        idField.setAccessible(true);

        SaveContext ctx = buildSaveContext(idField);

        return args -> doSave(ctx, args[0]);
    }

    private SaveContext buildSaveContext(Field idField) {
        boolean autoIncrement = metadata.idAutoIncrement();

        List<FieldMeta> nonIdFields = new ArrayList<>();
        List<FieldMeta> insertFields = new ArrayList<>();

        for (FieldMeta fieldMeta : metadata.fields()) {
            Field field = fieldMeta.field();
            field.setAccessible(true);

            boolean isId = field.equals(idField);

            if (!isId) {
                nonIdFields.add(fieldMeta);
            }
            if (!isId || !autoIncrement) {
                insertFields.add(fieldMeta);
            }
        }

        String table = metadata.tableName();
        String idColumn = metadata.idColumnName();

        String insertSql = "INSERT INTO " + table + " ("
                + insertFields.stream().map(FieldMeta::columnName).collect(Collectors.joining(", "))
                + ") VALUES ("
                + insertFields.stream().map(f -> "?").collect(Collectors.joining(", "))
                + ")";

        String updateSql = "UPDATE " + table + " SET "
                + nonIdFields.stream().map(f -> f.columnName() + " = ?").collect(Collectors.joining(", "))
                + " WHERE " + idColumn + " = ?";

        String existsSql = "SELECT 1 FROM " + table + " WHERE " + idColumn + " = ?";

        return new SaveContext(idField, insertFields, nonIdFields, insertSql, updateSql, existsSql);
    }

    private Object doSave(SaveContext ctx, Object entity) {
        try {
            Object idValue = ctx.idField().get(entity);
            boolean autoIncrement = metadata.idAutoIncrement();

            if (idValue == null) {
                if (!autoIncrement) {
                    throw new InvalidEntityException(
                            "Поле ID " + ctx.idField().getName() + " не задано, а сущность "
                                    + metadata.entityClass().getSimpleName() + " не auto-increment"
                    );
                }
                insertAndSetGeneratedId(ctx, entity);
                return entity;
            }

            if (autoIncrement || existsById(ctx, idValue)) {
                executeUpdate(ctx, entity, idValue);
            } else {
                executeInsert(ctx, entity);
            }

            return entity;
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(
                    "Не удалось сохранить " + metadata.entityClass().getName(), exception
            );
        }
    }

    private void insertAndSetGeneratedId(SaveContext ctx, Object entity) throws IllegalAccessException {
        Object generatedId = jdbi.withHandle(handle -> {
            Update update = handle.createUpdate(ctx.insertSql());
            bindFields(update, ctx.insertFields(), entity);
            return findFirstOrNull(
                    update.executeAndReturnGeneratedKeys().mapTo(ctx.idField().getType())
            );
        });

        if (generatedId != null) {
            ctx.idField().set(entity, generatedId);
        }
    }

    private void executeInsert(SaveContext ctx, Object entity) throws IllegalAccessException {
        jdbi.useHandle(handle -> {
            Update update = handle.createUpdate(ctx.insertSql());
            bindFields(update, ctx.insertFields(), entity);
            update.execute();
        });
    }

    private void executeUpdate(SaveContext ctx, Object entity, Object idValue) throws IllegalAccessException {
        jdbi.useHandle(handle -> {
            Update update = handle.createUpdate(ctx.updateSql());
            bindFields(update, ctx.nonIdFields(), entity);
            update.bind(ctx.nonIdFields().size(), idValue);
            update.execute();
        });
    }

    private boolean existsById(SaveContext ctx, Object idValue) {
        return jdbi.withHandle(handle ->
                handle.createQuery(ctx.existsSql())
                        .bind(0, idValue)
                        .mapTo(Integer.class)
                        .findFirst()
                        .isPresent()
        );
    }

    private void bindFields(Update update, List<FieldMeta> fields, Object entity) throws IllegalAccessException {
        for (int i = 0; i < fields.size(); i++) {
            update.bind(i, fields.get(i).field().get(entity));
        }
    }

    private boolean containsPositionalPlaceholder(String sql) {
        boolean insideLiteral = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (c == '\'') {
                insideLiteral = !insideLiteral;
                continue;
            }

            if (c == '?' && !insideLiteral) {
                return true;
            }
        }

        return false;
    }

    private Object handleQuery(
            Query annotation,
            ReturnTypeInfo type,
            ParamBinding[] bindings,
            Object[] args
    ) {
        Supplier<Object> supplier = () -> {

            if (type.hasWrapper(WrapperType.ITERABLE)) {
                return handleIterable(
                        annotation.value(),
                        type.getEntityType(),
                        bindings,
                        args
                );
            }

            Optional<?> result = handleOptionalQuery(
                    annotation.value(),
                    type.getEntityType(),
                    bindings,
                    args
            );

            return type.hasWrapper(WrapperType.OPTIONAL)
                    ? result
                    : result.orElseThrow(() ->
                                         new NoResultException("Запись не найдена"));
        };

        if (type.hasWrapper(WrapperType.ASYNC)) {
            return CompletableFuture.supplyAsync(supplier, executorService);
        }

        return supplier.get();
    }

    private Object handleUpdate(
            Modifying annotation,
            ReturnTypeInfo type,
            ParamBinding[] bindings,
            Object[] args
    ) {
        Supplier<Object> supplier = () -> {

            Optional<Object> result =
                    handleUpdate(annotation.value(), type, bindings, args);

            if (type.getEntityType() == void.class) {
                return null;
            }

            return type.hasWrapper(WrapperType.OPTIONAL)
                    ? result
                    : result.orElseThrow(() ->
                                         new NoResultException("Запись не найдена"));
        };

        if (type.hasWrapper(WrapperType.ASYNC)) {
            return CompletableFuture.supplyAsync(supplier, executorService);
        }

        return supplier.get();
    }

    private Optional<Object> handleUpdate(String value, ReturnTypeInfo typeInfo, ParamBinding[] bindings, Object[] args) {
        if (typeInfo.entityType == void.class || Void.class.isAssignableFrom(typeInfo.entityType)) {
            jdbi.withHandle(handle ->
                    prepareParameters(handle.createUpdate(value), bindings, args)
                            .execute()
            );
            return Optional.empty();
        }

        return Optional.ofNullable(jdbi.withHandle(handle -> findFirstOrNull(
                        prepareParameters(handle.createUpdate(value), bindings, args)
                                .executeAndReturnGeneratedKeys()
                                .mapTo(typeInfo.entityType)
                )

        ));
    }

    @Nullable
    private <T> T findFirstOrNull(ResultIterable<T> iterable) {
        return iterable.findFirst().orElse(null);
    }

    private Optional<?> handleOptionalQuery(String value, Class<?> returnType, ParamBinding[] bindings, Object[] args) {
        return jdbi.withHandle(handle ->
                prepareResultIterable(handle, value, returnType, bindings, args)
                        .findFirst()
        );
    }

    private Iterable<?> handleIterable(String value, Class<?> returnType, ParamBinding[] bindings, Object[] args) {
        return jdbi.withHandle(handle ->
                prepareResultIterable(handle, value, returnType, bindings, args)
                        .list()
        );
    }

    private ResultIterable<?> prepareResultIterable(Handle handle, String value, Class<?> returnType, ParamBinding[] bindings, Object[] args) {
        return prepareParameters(handle.createQuery(value), bindings, args)
                .mapTo(returnType);
    }

    @NotNull
    private <T extends SqlStatement<?>> T prepareParameters(T statement, ParamBinding[] bindings, Object[] args) {
        for (int i = 0; i < bindings.length; i++) {
            ParamBinding binding = bindings[i];

            if (binding.name != null) {
                statement.bind(binding.name, args[i]);
            } else {
                statement.bind(binding.index, args[i]);
            }
        }
        return statement;
    }

    private ReturnTypeInfo parse(Method method) {
        ReturnTypeInfo info = new ReturnTypeInfo();
        parse(method.getGenericReturnType(), info);
        return info;
    }

    private void parse(Type type, ReturnTypeInfo info) {

        if (type instanceof Class<?>) {
            info.setEntityType((Class<?>) type);
            return;
        }

        if (!(type instanceof ParameterizedType)) {
            throw new RuntimeException("Не удалось разобрать тип " + type);
        }

        ParameterizedType parameterized = (ParameterizedType) type;
        Class<?> raw = (Class<?>) parameterized.getRawType();

        WrapperType wrapper = WrapperType.of(raw);

        if (wrapper == null) {
            throw new UnsupportedOperationException(
                    "Контейнер " + raw.getName() + " не поддерживается"
            );
        }

        info.addWrapper(wrapper);
        parse(parameterized.getActualTypeArguments()[0], info);
    }

    @Getter
    public static final class ReturnTypeInfo {

        private final List<WrapperType> wrappers = new ArrayList<>();
        private Class<?> entityType;

        void addWrapper(WrapperType wrapper) {
            wrappers.add(wrapper);
        }

        void setEntityType(Class<?> entityType) {
            this.entityType = entityType;
        }

        public boolean hasWrapper(WrapperType wrapper) {
            return wrappers.contains(wrapper);
        }
    }

    private static final class ParamBinding {
        private final String name;
        private final int index;

        private ParamBinding(String name, int index) {
            this.name = name;
            this.index = index;
        }

        static ParamBinding named(String name) {
            return new ParamBinding(name, -1);
        }

        static ParamBinding positional(int index) {
            return new ParamBinding(null, index);
        }
    }

    @Getter
    public enum WrapperType {
        ASYNC(CompletableFuture.class),
        OPTIONAL(Optional.class),
        ITERABLE(Iterable.class);

        private final Class<?> type;

        WrapperType(Class<?> type) {
            this.type = type;
        }

        public static WrapperType of(Class<?> clazz) {
            for (WrapperType wrapper : values()) {
                if (wrapper.type.isAssignableFrom(clazz)) {
                    return wrapper;
                }
            }
            return null;
        }
    }
}