// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.libs.player.PlayerSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
@Slf4j
@RequiredArgsConstructor
@Service
public class LuckPermsService {
    private final LuckPerms luckPerms;

    public CompletableFuture<Boolean> hasPermission(PlayerSnapshot snapshot, String permission){
        return hasPermission(snapshot.uuid(), permission);
    }

    public CompletableFuture<Boolean> hasPermission(Player player, String permission){
        return hasPermission(player.getUniqueId(), permission);
    }

    public CompletableFuture<Boolean> hasPermission(UUID uuid, String permission) {

        UserManager userManager = luckPerms.getUserManager();

        User user = userManager.getUser(uuid);

        // already cached
        if (user != null) {
            boolean result = user.getCachedData()
                    .getPermissionData()
                    .checkPermission(permission)
                    .asBoolean();

            return CompletableFuture.completedFuture(result);
        }

        // load async
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        userManager.loadUser(uuid).thenAccept(loaded -> {

            boolean result = loaded.getCachedData()
                    .getPermissionData()
                    .checkPermission(permission)
                    .asBoolean();

            future.complete(result);
        }).exceptionally(ex -> {
            future.complete(false);
            log.error("Error while loading user with uuid: {}", uuid, ex);
            return null;
        });

        return future;
    }
}
