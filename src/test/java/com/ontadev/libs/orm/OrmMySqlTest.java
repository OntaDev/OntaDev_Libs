// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import ch.vorburger.exec.ManagedProcessException;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тот же путь save/find, что и в {@link OrmTest}, но на живой MariaDB —
 * поднятой embedded-процессом (MariaDB4j, без Docker) и адресуемой по протоколу
 * "jdbc:mysql://..." (MariaDB wire-совместим с MySQL), чтобы поймать
 * MySQL-диалект-специфичные баги DDL (AUTO_INCREMENT и т.д.), которых
 * H2/SQLite/Postgres не покрывают. MariaDB — не 100% то же самое, что MySQL,
 * но синтаксически достаточно близко для проверки нашего Dialect-кода.
 */
public class OrmMySqlTest {

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
    void shouldGenerateSchemaAndSaveOnMySql() throws Exception {
        DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(0);

        DB db = startOrSkip(configBuilder);

        try {
            db.createDB("ontadev_test");

            IoCContainer container = mock(IoCContainer.class);

            // configBuilder.getURL() отдаёт "jdbc:mariadb://..." — у нас в classpath только
            // mysql-connector-j, а не mariadb-java-client, поэтому собираем URL под mysql-драйвер вручную
            // (MariaDB wire-совместим с MySQL, драйвер этого не заметит).
            String jdbcUrl = "jdbc:mysql://localhost:" + configBuilder.getPort() + "/ontadev_test";

            DatabaseManager databaseManager = new DatabaseManager(new SettingsConfig(new DatabaseSettings(
                    jdbcUrl,
                    "root",
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
        } finally {
            db.stop();
        }
    }

    /** На платформах без готового бинарника (например, экзотичная архитектура) тест мягко скипается. */
    private static DB startOrSkip(DBConfigurationBuilder configBuilder) {
        try {
            DB db = DB.newEmbeddedDB(configBuilder.build());
            db.start();
            return db;
        } catch (ManagedProcessException | RuntimeException exception) {
            Assumptions.abort("Embedded MariaDB недоступна на этой платформе: " + exception.getMessage());
            throw new AssertionError("unreachable");
        }
    }
}
