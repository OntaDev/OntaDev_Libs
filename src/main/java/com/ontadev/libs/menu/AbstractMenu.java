// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.menu;

import com.ontadev.libs.item.ItemModel;
import com.ontadev.libs.menu.manager.MenuManager;
import com.ontadev.libs.menu.task.MenuTask;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.message.Placeholders;
import com.ontadev.libs.player.PlayerSnapshot;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class AbstractMenu {

    @Setter
    private MenuManager menuManager;

    /**
     * Кэш "слот -> ItemModel" из {@link #staticItems()}.
     */
    private Map<Integer, ItemModel> cachedStaticItems;

    /**
     * Кэш "слот -> ItemStack" для статических предметов.
     */
    private Map<Integer, ItemStack> cachedStaticItemStacks;

    /**
     * Могут ли игроки свободно перемещать/класть/брать предметы в слотах без
     * зарегистрированного {@link ItemModel}. По умолчанию {@code false}: любой клик
     * по неуправляемому слоту отменяется, делая весь инвентарь доступным только для чтения,
     * если слот явно не разрешает иное.
     */
    @Getter
    protected boolean editable = false;

    public abstract Message title(PlayerSnapshot snapshot);

    public abstract int size();

    public InventoryType inventoryType(){return  null;}

    /**
     * Предметы, общие для всех зрителей меню (границы, кнопки закрытия,
     * иконки категорий и т.д.). Определите здесь статическую раскладку.
     * Возвращайте {@code null} (по умолчанию), если у меню нет статических предметов.
     */
    public Map<Integer, ItemModel> staticItems() {
        return null;
    }

    /**
     * Предметы, зависящие от игрока (балансы, кулдауны, предметы игрока и т.д.).
     * Слоты, указанные здесь, переопределяют те же слоты из {@link #staticItems()}.
     * Возвращайте {@code null} (по умолчанию), если у меню нет динамических предметов.
     */
    public Map<Integer, ItemModel> dynamicItems(PlayerSnapshot snapshot) {
        return null;
    }

    /**
     * Открывает меню для игрока.
     */
    public void open(PlayerSnapshot snapshot) {
        manager().open(this, snapshot);
    }

    /**
     * Открывает меню для игрока.
     */
    public void open(Player player) {
        manager().open(this, player);
    }

    /**
     * Закрывает меню для игрока, если оно открыто.
     */
    public void close(PlayerSnapshot snapshot) {
        manager().close(snapshot);
    }

    /**
     * Собирает итоговую карту "слот -> предмет" для игрока.
     */
    final Map<Integer, ItemModel> resolveItems(PlayerSnapshot snapshot) {
        Map<Integer, ItemModel> resolved = new HashMap<>(resolvedStaticItems());

        Map<Integer, ItemModel> dynamic = dynamicItems(snapshot);
        if (dynamic != null) {
            resolved.putAll(dynamic);
        }

        return resolved;
    }

    public Placeholders staticPlaceholders(){
        return null;
    }

    public Placeholders dynamicPlaceholders(PlayerSnapshot playerSnapshot){
        return null;
    }

    /**
     * Возвращает кэшированную карту из {@link #staticItems()}.
     */
    final synchronized Map<Integer, ItemModel> resolvedStaticItems() {
        if (cachedStaticItems == null) {
            Map<Integer, ItemModel> items = staticItems();
            cachedStaticItems = (items == null) ? Collections.emptyMap() : Map.copyOf(items);
        }
        return cachedStaticItems;
    }

    /**
     * Возвращает кэшированную карту предметов, отрендеренных из статических моделей.
     */
    public final synchronized Map<Integer, ItemStack> resolvedStaticItemStacks() {
        if (cachedStaticItemStacks == null) {
            Map<Integer, ItemStack> stacks = new HashMap<>();
            resolvedStaticItems().forEach((slot, model) -> stacks.put(slot, model.toItemStack()));
            cachedStaticItemStacks = Map.copyOf(stacks);
        }
        return cachedStaticItemStacks;
    }

    /**
     * Сбрасывает кэш статических предметов.
     */
    protected final synchronized void invalidateStaticItems() {
        cachedStaticItems = null;
        cachedStaticItemStacks = null;
    }

    private MenuManager manager() {
        if (menuManager == null) {
            throw new IllegalStateException(
                    "Menu " + getClass().getSimpleName() + " не зарегистрировано в MenuManager"
            );
        }
        return menuManager;
    }

    /**
     * Период автоматического обновления dynamicItems().
     * <= 0 — обновление отключено.
     */
    public long refreshPeriod() {
        return -1L;
    }

    /**
     * Дополнительные задачи, запускаемые при открытии меню.
     * Все задачи автоматически отменяются при закрытии меню.
     */
    public Collection<MenuTask> tasks() {
        return Collections.emptyList();
    }

    public String id() {
        return getClass().getName();
    }

}