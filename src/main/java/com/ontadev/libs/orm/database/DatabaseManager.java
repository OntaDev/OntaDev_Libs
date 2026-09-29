// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.database;

import com.ontadev.libs.config.SettingsConfig;
import com.ontadev.libs.orm.adapter.JsonTypeAdapter;
import com.ontadev.libs.orm.adapter.UuidTypeAdapter;
import com.ontadev.libs.orm.dto.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public final class DatabaseManager {
    private final HikariDataSource dataSource;
    @Getter
    private final Dialect dialect;
    @Getter
    private final Jdbi jdbi;

    public DatabaseManager(SettingsConfig settingsConfig) {
        DatabaseSettings config = settingsConfig.getDatabaseSettings();

        applyH2Fallback(config);

        this.dialect = Dialect.fromJdbcUrl(config.getUrl());

        HikariConfig hikariConfig = getHikariConfig(config, dialect);

        this.dataSource = new HikariDataSource(hikariConfig);

        this.jdbi = Jdbi.create(dataSource);

        new UuidTypeAdapter().registerOn(jdbi);
        new JsonTypeAdapter().registerOn(jdbi);

        log.info("DatabaseManager инициализирован: {} (диалект: {})", config.getUrl(), dialect);
    }

    /**
     * Если URL не указан в конфиге, откатываемся на H2 по умолчанию,
     * чтобы плагин не падал из-за пустого конфига.
     */
    private static void applyH2Fallback(DatabaseSettings config) {
        if (config.getUrl() == null || config.getUrl().isBlank()) {
            log.warn("URL базы данных не указан в конфиге, использую H2 по умолчанию: {}",
                    DatabaseSettings.DEFAULT_H2_URL);
            config.setUrl(DatabaseSettings.DEFAULT_H2_URL);
        }
    }

    private static String resolveDriverClassName(Dialect dialect) {
        switch (dialect) {
            case MYSQL:
                return "com.mysql.cj.jdbc.Driver";
            case POSTGRESQL:
                return "org.postgresql.Driver";
            case H2:
            case H2_POSTGRES:
                return "org.h2.Driver";
            default:
                throw new IllegalStateException("Unexpected database type: " + dialect);
        }
    }

    private static @NotNull HikariConfig getHikariConfig(DatabaseSettings config, Dialect dialect) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setDriverClassName(resolveDriverClassName(dialect));

        if (config.getUsername() != null) {
            hikariConfig.setUsername(config.getUsername());
        }
        if (config.getPassword() != null) {
            hikariConfig.setPassword(config.getPassword());
        }

        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(config.getMinimumIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeoutMs());
        hikariConfig.setIdleTimeout(config.getIdleTimeoutMs());
        hikariConfig.setMaxLifetime(config.getMaxLifetimeMs());
        hikariConfig.setPoolName("OntaDev-Libs-Pool");
        return hikariConfig;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("DatabaseManager: пул соединений закрыт");
        }
    }
}