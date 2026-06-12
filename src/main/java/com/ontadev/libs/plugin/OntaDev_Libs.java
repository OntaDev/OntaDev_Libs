// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT
package com.ontadev.libs.plugin;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public final class OntaDev_Libs extends OntaDev_Template {
    @Getter
    public static OntaDev_Libs instance;

    @Override
    public void onEnable() {
        log.info("[OntaDevLibs] Enabled");

        log.info(getPluginIoC().get(JavaPlugin.class).getName());
    }

}
