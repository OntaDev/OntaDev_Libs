// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.config;

import com.google.gson.annotations.SerializedName;
import com.ontadev.libs.orm.dto.DatabaseSettings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SettingsConfig extends YamlConfig{

    @SerializedName("database-settings")
    DatabaseSettings databaseSettings = new DatabaseSettings();


    @Override
    public String getFileName() {
        return "settings";
    }
}
