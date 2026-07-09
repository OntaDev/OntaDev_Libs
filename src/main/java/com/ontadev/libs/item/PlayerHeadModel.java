// PPFSS_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Getter
@Setter
public class PlayerHeadModel extends ItemModel {

    private String base64Texture;
    private OfflinePlayer owner;

    public PlayerHeadModel() {
        setMaterial(Material.PLAYER_HEAD);
    }

    /**
     * Устанавливает Base64-текстуру головы игрока.
     */
    public PlayerHeadModel texture(String base64) {
        this.base64Texture = base64;
        this.owner = null;
        return this;
    }

    /**
     * Устанавливает владельца головы.
     */
    public PlayerHeadModel owner(OfflinePlayer owner) {
        this.owner = owner;
        this.base64Texture = null;
        return this;
    }

    /**
     * Устанавливает владельца головы по UUID.
     */
    public PlayerHeadModel owner(UUID uuid) {
        return owner(Bukkit.getOfflinePlayer(uuid));
    }

    /**
     * Устанавливает владельца головы по имени игрока.
     */
    @Deprecated
    public PlayerHeadModel owner(String name) {
        return owner(Bukkit.getOfflinePlayer(name));
    }

    @Override
    public ItemStack toItemStack() {
        ItemStack item = super.toItemStack();

        if (!(item.getItemMeta() instanceof SkullMeta)) {
            return item;
        }

        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (owner != null) {
            meta.setOwningPlayer(owner);
        } else if (base64Texture != null && !base64Texture.isBlank()) {
            PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(base64Texture.getBytes(StandardCharsets.UTF_8)));

            profile.setProperty(new ProfileProperty(
                    "textures",
                    base64Texture
            ));

            meta.setPlayerProfile(profile);
        }

        item.setItemMeta(meta);
        return item;
    }
}