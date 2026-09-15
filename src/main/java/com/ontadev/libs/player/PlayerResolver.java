// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.player;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class PlayerResolver {
    private final JavaPlugin plugin;

    public PlayerSnapshot getPlayerSnapshot(@NotNull Player player) {
        return new PlayerSnapshot(
                player.getUniqueId(),
                player.getName()
        );
    }


    public CompletableFuture<Player> getPlayer(PlayerSnapshot snapshot){
        if (Bukkit.isPrimaryThread()){
            return CompletableFuture.completedFuture(
                    Bukkit.getPlayer(snapshot.uuid())
            );
        }

        CompletableFuture<Player> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, ()->{
            future.complete(
                    Bukkit.getPlayer(snapshot.uuid())
            );
        });

        return future;
    }
}
