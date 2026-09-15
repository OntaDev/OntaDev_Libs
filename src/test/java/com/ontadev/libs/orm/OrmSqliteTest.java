// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm;

import com.ontadev.libs.config.SettingsConfig;
import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.orm.adapter.EntityRowMapperFactory;
import com.ontadev.libs.orm.annotation.Query;
import com.ontadev.libs.orm.annotation.entity.Column;
import com.ontadev.libs.orm.annotation.entity.Entity;
import com.ontadev.libs.orm.annotation.entity.GeneratedValue;
import com.ontadev.libs.orm.annotation.entity.Id;
import com.ontadev.libs.orm.annotation.entity.Table;
import com.ontadev.libs.orm.database.DatabaseManager;
import com.ontadev.libs.orm.dto.DatabaseSettings;
import com.ontadev.libs.orm.entity.SchemaGenerator;
import com.ontadev.libs.orm.mapper.EntityMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тот же путь save/find, что и в {@link OrmTest}, но на живом SQLite —
 * H2 и SQLite генерируют разный DDL для auto-increment PK
 * ({@link com.ontadev.libs.orm.database.Dialect#autoIncrementPrimaryKey()}),
 * поэтому H2-тест сам по себе не гарантирует, что схема реально накатится на SQLite.
 */
public class OrmSqliteTest {

    @Entity
    @Table("players")
    static class Player {

        @Id
        @GeneratedValue
        private Long id;

        @Column
        private String nickname;
    }

    interface PlayerRepository extends OrmRepository<Player, Long> {

        @Query("SELECT * FROM players WHERE nickname = ?")
        Optional<Player> findByNickname(String nickname);
    }

    @Test
    void shouldGenerateSchemaAndSaveOnSqlite() throws IOException {
        File dbFile = File.createTempFile("ontadev-orm-test-", ".db");
        dbFile.deleteOnExit();

        IoCContainer container = mock(IoCContainer.class);

        DatabaseManager databaseManager = new DatabaseManager(new SettingsConfig(new DatabaseSettings(
                "jdbc:sqlite:" + dbFile.getAbsolutePath(),
                "",
                "",
                1,
                1,
                30000L,
                600000L,
                1800000L
        )));

        EntityMapper entityMapper = new EntityMapper(databaseManager.getDialect());
        SchemaGenerator generator = new SchemaGenerator(databaseManager, entityMapper);

        EntityRowMapperFactory entityRowMapperFactory = new EntityRowMapperFactory(entityMapper);
        databaseManager.getJdbi().registerRowMapper(entityRowMapperFactory);

        when(container.get(DatabaseManager.class)).thenReturn(databaseManager);
        when(container.get(SchemaGenerator.class)).thenReturn(generator);
        when(container.get(EntityMapper.class)).thenReturn(entityMapper);

        PlayerRepository result = new OrmRepositoryHandler().createRealization(container, PlayerRepository.class);

        Player player = new Player();
        player.nickname = "steve-" + UUID.randomUUID();

        Player saved = result.save(player);
        Assertions.assertNotNull(saved.id);

        Optional<Player> found = result.findByNickname(player.nickname);
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(saved.id, found.get().id);

        databaseManager.shutdown();
    }
}
