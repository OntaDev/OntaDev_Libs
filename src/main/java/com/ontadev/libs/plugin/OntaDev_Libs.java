// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT
package com.ontadev.libs.plugin;

import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.message.Message;
import lombok.Getter;

@SuppressWarnings("unused")
public final class OntaDev_Libs extends OntaDev_Template {
    @Getter
    public static OntaDev_Libs instance;

    @Override
    public void onLoad(){
        Message.load(this);

        super.onLoad();
    }


    @Override
    public void onPluginEnable(PluginIoC pluginIoC) {
        log.info("[OntaDevLibs] Enabled");
    }

}
