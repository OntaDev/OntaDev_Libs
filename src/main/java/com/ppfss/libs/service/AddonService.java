// PPFSS_Libs Plugin 
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.service;

import com.ppfss.libs.ioc.annotation.Component;
import com.ppfss.libs.ioc.annotation.Inject;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AddonService {
    @Getter
    private static boolean LUCK_PERMS = false;

    @Inject
    public AddonService(ProxyServer server) {

        LUCK_PERMS = server.getPluginManager().getPlugin("LuckPerms").isPresent();

        log.info("LuckPerms is {}", LUCK_PERMS);
    }
}
