// OntaDev_Libs Plugin 
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.service;

import com.ontadev.libs.ioc.annotation.Component;
import com.ontadev.libs.ioc.annotation.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.java.JavaPlugin;

@Slf4j
@Component
public class AddonService {

    @Getter
    private static boolean LUCK_PERMS = false;

    @Inject
    public AddonService(JavaPlugin plugin) {

        LUCK_PERMS = plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null;

        log.info("LuckPerms is {}", LUCK_PERMS);
    }
}
