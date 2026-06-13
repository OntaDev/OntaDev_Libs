// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.plugin;

import com.ontadev.libs.ioc.PluginIoC;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OntaDev_Template extends JavaPlugin {

    @Getter
    private static PluginIoC pluginIoC;

    protected static Logger log;

    @Override
    public void onLoad() {
        log = LoggerFactory.getLogger(this.getClass());

    }

    @Override
    public void onDisable() {
        pluginIoC.shutdownPlugin();
    }


    @Override
    public void onEnable() {
        pluginIoC = new PluginIoC(this);

        pluginIoC.onEnable();

        onPluginEnable(pluginIoC);
    }

    public abstract void onPluginEnable(PluginIoC pluginIoC);
}
