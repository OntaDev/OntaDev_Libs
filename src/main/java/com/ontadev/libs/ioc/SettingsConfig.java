// PPFSS_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc;

import com.ontadev.libs.config.YamlConfig;
import com.ontadev.libs.ioc.annotation.stereotype.Config;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Config
public class SettingsConfig extends YamlConfig {
    private boolean debug = false;

    @Override
    public String getFileName() {
        return "settings.yml";
    }
}
