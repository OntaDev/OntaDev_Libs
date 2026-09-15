// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.mapper;

import com.ontadev.libs.exception.InvalidEntityException;
import com.ontadev.libs.orm.annotation.entity.Column;
import com.ontadev.libs.orm.annotation.entity.GeneratedValue;
import com.ontadev.libs.orm.annotation.entity.Id;
import com.ontadev.libs.orm.database.Dialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EntityMapperTest {

    static class TwoIds {
        @Id
        private Long id;

        @Id
        private Long secondId;
    }

    static class NoDefaultConstructor {
        @Id
        @GeneratedValue
        private Long id;

        NoDefaultConstructor(Long id) {
            this.id = id;
        }
    }

    static class UnsupportedFieldType {
        @Id
        @GeneratedValue
        private Long id;

        @Column
        private Thread notMappable;
    }

    static class ValidEntity {
        @Id
        @GeneratedValue
        private Long id;

        @Column
        private String name;
    }

    @Test
    void shouldThrowWhenEntityHasMultiplePrimaryKeys() {
        EntityMapper mapper = new EntityMapper(Dialect.H2);

        Assertions.assertThrows(InvalidEntityException.class, () -> mapper.metadataOf(TwoIds.class));
    }

    @Test
    void shouldThrowWhenEntityHasNoDefaultConstructor() {
        EntityMapper mapper = new EntityMapper(Dialect.H2);

        Assertions.assertThrows(IllegalStateException.class, () -> mapper.metadataOf(NoDefaultConstructor.class));
    }

    @Test
    void shouldThrowWhenFieldTypeIsUnknownAndNotOverridden() {
        EntityMapper mapper = new EntityMapper(Dialect.H2);

        Assertions.assertThrows(IllegalArgumentException.class, () -> mapper.metadataOf(UnsupportedFieldType.class));
    }

    @Test
    void shouldBuildMetadataForValidEntity() {
        EntityMapper mapper = new EntityMapper(Dialect.H2);

        EntityMetadata metadata = mapper.metadataOf(ValidEntity.class);

        Assertions.assertEquals("valid_entity", metadata.tableName());
        Assertions.assertEquals("id", metadata.idColumnName());
        Assertions.assertTrue(metadata.idAutoIncrement());
        Assertions.assertEquals(2, metadata.fields().size());
    }
}
