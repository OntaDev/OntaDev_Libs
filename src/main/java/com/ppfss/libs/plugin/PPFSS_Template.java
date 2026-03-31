// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.plugin;

import com.ppfss.libs.ioc.PluginIoC;
import com.ppfss.libs.message.Message;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.slf4j.Logger;

import java.nio.file.Path;


public abstract class PPFSS_Template {
    @Getter
    private static PluginIoC pluginIoC;

    protected final PluginContainer container;
    protected final ProxyServer server;
    protected final Path dataDirectory;
    protected final Logger log;


    public PPFSS_Template(ProxyServer server, Logger logger,
                          PluginContainer container, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.log = logger;
        this.container = container;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Message.load(server, this);
        pluginIoC = new PluginIoC(this, server, dataDirectory, container);
    }
}
