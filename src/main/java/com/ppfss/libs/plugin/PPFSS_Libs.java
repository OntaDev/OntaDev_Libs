// PPFSS_Libs Plugin
// Авторские права (c) 2025 PPFSS
// Лицензия: MIT
package com.ppfss.libs.plugin;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "ppfss_libs", name = "PPFSS_Libs", authors = "PPFSS", version = "0.0.6")
@SuppressWarnings("unused")
public final class PPFSS_Libs extends PPFSS_Template {
    @Getter
    public static PPFSS_Libs instance;

    @Inject
    public PPFSS_Libs(ProxyServer server, Logger logger, PluginContainer container, @DataDirectory Path dataDirectory) {
        super(server, logger, container, dataDirectory);
    }
}
