// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.menu;

import com.ontadev.libs.item.ItemModel;
import com.ontadev.libs.player.PlayerSnapshot;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

/**
 * Состояние {@link AbstractMenu} для конкретного игрока и текущего сеанса открытия меню.
 * Создается в {@link MenuManagerImpl#open} и удаляется после закрытия инвентаря.
 */
@Getter
public final class MenuSession {

    private final AbstractMenu abstractMenu;
    private final Inventory inventory;
    private final PlayerSnapshot playerSnapshot;
    private final Player player;

    @Getter
    @Setter
    private boolean closed;

    /** Текущая карта отображения «слот -> ItemModel»; заменяется при каждом вызове {@link MenuManagerImpl#refresh}. */    @Setter
    private Map<Integer, ItemModel> items;

    MenuSession(AbstractMenu abstractMenu, Inventory inventory, Map<Integer, ItemModel> items, PlayerSnapshot playerSnapshot, Player player) {
        this.abstractMenu = abstractMenu;
        this.inventory = inventory;
        this.items = items;
        this.playerSnapshot = playerSnapshot;
        this.player = player;
    }

    ItemModel itemAt(int rawSlot) {
        return items.get(rawSlot);
    }
}