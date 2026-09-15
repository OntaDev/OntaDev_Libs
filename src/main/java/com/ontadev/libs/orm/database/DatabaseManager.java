// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.database;

import com.ontadev.libs.config.SettingsConfig;
import com.ontadev.libs.ioc.IoCContainer;
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

        HikariConfig hikariConfig = getHikariConfig(config);

        this.dataSource = new HikariDataSource(hikariConfig);
        this.dialect = Dialect.fromJdbcUrl(config.getUrl());

        this.jdbi = Jdbi.create(dataSource);

        log.info("DatabaseManager инициализирован: {} (диалект: {})", config.getUrl(), dialect);
    }

    private static @NotNull HikariConfig getHikariConfig(DatabaseSettings config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getUrl());

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