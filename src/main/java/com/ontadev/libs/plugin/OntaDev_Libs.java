// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT
package com.ontadev.libs.plugin;

import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.menu.MenuManagerImpl;
import com.ontadev.libs.menu.manager.MenuManager;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.player.PlayerResolver;
import lombok.Getter;

@SuppressWarnings("unused")
public final class OntaDev_Libs extends OntaDev_Template {
    @Getter
    public static OntaDev_Libs instance;

    public static MenuManager defaultMenuManager;
    public static PlayerResolver playerResolver;

    @Override
    public void onLoad() {
        Message.load(this);

        createDefaultMenuManager();

        super.onLoad();
    }


    private void createDefaultMenuManager() {
        IoCContainer container = getPluginIoC().getContainer();
        playerResolver = container.create(PlayerResolver.class);
        defaultMenuManager = container.create(MenuManagerImpl.class);
    }

    @Override
    public void onPluginEnable(PluginIoC pluginIoC) {
        log.info("[OntaDevLibs] Enabled");
    }

}
