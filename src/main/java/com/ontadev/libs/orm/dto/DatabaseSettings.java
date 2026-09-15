// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseSettings {
    private String url = "jdbc:sqlite:plugins/OntaDev_Libs/database.db";
    private String username = "";
    private String password = "";
    @SerializedName("maximum-pool-size")
    private int maximumPoolSize = 10;
    @SerializedName("minimum-idle")
    private int minimumIdle = 2;
    @SerializedName("connection-timeout-ms")
    private long connectionTimeoutMs = 10_000;
    @SerializedName("idle-timeout-ms")
    private long idleTimeoutMs = 600_000;
    @SerializedName("max-lifetime-ms")
    private long maxLifetimeMs = 1_800_000;
}