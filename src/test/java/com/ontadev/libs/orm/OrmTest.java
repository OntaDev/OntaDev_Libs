// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm;

import com.ontadev.libs.config.SettingsConfig;
import com.ontadev.libs.exception.NoResultException;
import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.orm.adapter.EntityRowMapperFactory;
import com.ontadev.libs.orm.adapter.JsonTypeAdapter;
import com.ontadev.libs.orm.adapter.UuidTypeAdapter;
import com.ontadev.libs.orm.annotation.Query;
import com.ontadev.libs.orm.annotation.Modifying;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class OrmTest {
    private final Logger log = Logger.getLogger("orm-test");

    private IoCContainer container;
    private DatabaseManager databaseManager;
    private EntityMapper entityMapper;
    private SchemaGenerator generator;

    @Entity
    @Table("users")
    static class User {

        @Id
        @GeneratedValue
        private Long id;

        @Column
        private String name;

        @Column
        private UUID externalId;

        @Column(type = "TEXT")
        private Map<String, String> metadata;
    }

    interface TestRepository extends OrmRepository<User, Long> {

        @Query("SELECT * FROM users WHERE id = ?")
        Optional<User> findById(Long id);

        @Query("SELECT * FROM users WHERE name = ?")
        Optional<User> findByName(String name);

        @Query("SELECT * FROM users WHERE name = ?")
        User getByName(String name);

        @Modifying("DELETE FROM users WHERE id = ?")
        void deleteById(Long id);
    }

    @Entity
    @Table("settings")
    static class Setting {

        @Id
        @Column("setting_key")
        private String key;

        @Column
        private String settingValue;
    }

    interface SettingRepository extends OrmRepository<Setting, String> {

        @Query("SELECT * FROM settings WHERE setting_key = ?")
        Optional<Setting> findByKey(String key);
    }

    @BeforeEach
    void setUp() {
        container = mock(IoCContainer.class);

        databaseManager = new DatabaseManager(new SettingsConfig(new DatabaseSettings(
                "jdbc:h2:mem:orm_test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "sa",
                "",
                2,
                1,
                30000L,
                600000L,
                1800000L
        )));

        new UuidTypeAdapter().registerOn(databaseManager.getJdbi());
        new JsonTypeAdapter().registerOn(databaseManager.getJdbi());

        entityMapper = new EntityMapper(databaseManager.getDialect());
        generator = new SchemaGenerator(databaseManager, entityMapper);

        EntityRowMapperFactory entityRowMapperFactory = new EntityRowMapperFactory(entityMapper);
        databaseManager.getJdbi().registerRowMapper(entityRowMapperFactory);

        when(container.get(DatabaseManager.class)).thenReturn(databaseManager);
        when(container.get(SchemaGenerator.class)).thenReturn(generator);
        when(container.get(EntityMapper.class)).thenReturn(entityMapper);
    }

    private <T> T createRepository(Class<T> iface) {
        return new OrmRepositoryHandler().createRealization(container, iface);
    }

    @Test
    void shouldCreateRepositoryRealization() {
        TestRepository result = createRepository(TestRepository.class);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(TestRepository.class, result);

        verify(container).get(DatabaseManager.class);
        verify(container).get(SchemaGenerator.class);
        verify(container).get(EntityMapper.class);

        User user = new User();
        user.name = "hello";
        user.externalId = UUID.randomUUID();
        user.metadata = new HashMap<>();
        user.metadata.put("level", "42");

        User saved = result.save(user);
        Assertions.assertNotNull(saved.id);
        log.info("Saved user id is " + saved.id);

        Optional<User> found = result.findByName("hello");

        found.ifPresentOrElse(u -> {
            log.info("User exists with id " + u.id);
            Assertions.assertEquals(user.externalId, u.externalId);
            Assertions.assertEquals("42", u.metadata.get("level"));
            log.info("Deleting user");
            result.deleteById(u.id);
        }, () -> {
            throw new IllegalStateException("User not exists");
        });

        Assertions.assertTrue(result.findByName("hello").isEmpty());
    }

    @Test
    void shouldReturnEmptyOptionalWhenNoMatch() {
        TestRepository result = createRepository(TestRepository.class);

        Assertions.assertTrue(result.findByName("ghost").isEmpty());
    }

    @Test
    void shouldThrowNoResultExceptionForNonOptionalQueryWithoutMatch() {
        TestRepository result = createRepository(TestRepository.class);

        Assertions.assertThrows(NoResultException.class, () -> result.getByName("ghost"));
    }

    @Test
    void shouldInsertThenUpdateEntityWithManualId() {
        SettingRepository result = createRepository(SettingRepository.class);

        Setting setting = new Setting();
        setting.key = "max-players";
        setting.settingValue = "20";

        // id задан вручную (не auto-increment) -> save() должен сделать INSERT
        Setting inserted = result.save(setting);
        Assertions.assertEquals("20", inserted.settingValue);

        Optional<Setting> afterInsert = result.findByKey("max-players");
        Assertions.assertTrue(afterInsert.isPresent());
        Assertions.assertEquals("20", afterInsert.get().settingValue);

        // тот же id -> save() должен сделать UPDATE, а не упасть на дублирующемся PK
        setting.settingValue = "32";
        result.save(setting);

        Optional<Setting> afterUpdate = result.findByKey("max-players");
        Assertions.assertTrue(afterUpdate.isPresent());
        Assertions.assertEquals("32", afterUpdate.get().settingValue);
    }
}
