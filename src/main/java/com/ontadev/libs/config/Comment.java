package com.ontadev.libs.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Добавляет YAML-комментарий перед ключом поля при создании конфига из класса.
 * Учитывает имя из {@link com.google.gson.annotations.SerializedName}.
 * Поддерживается на полях вложенных объектов и родительских классов.
 *
 * <p>Если конфиг копируется из ресурсов, аннотация не применяется.
 * При последующих сохранениях комментарии из аннотации не добавляются заново:
 * сохраняются комментарии, уже записанные в файле.</p>
 *
 * <pre>{@code
 * @Comment("Количество подключений\nМинимальное значение: 1")
 * private int connections = 10;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Comment {
    /**
     * Текст комментария без начального символа {@code #}.
     * Переводы строк разделяют комментарий на несколько строк.
     *
     * @return текст комментария; пустая строка не добавляет комментарий
     */
    String value();
}
