// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.menu.manager;

import com.ontadev.libs.menu.AbstractMenu;
import com.ontadev.libs.player.PlayerSnapshot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface MenuManager {

    /**
     * Регистрирует меню, привязывает его к данному менеджеру, а также
     * предварительно формирует и кэширует его статические {@link ItemStack} в главном потоке.
     */
    <T extends AbstractMenu> void registerMenu(T menu);

    /** Возвращает ранее зарегистрированное меню по его {@link AbstractMenu#id()} или {@code null}. */
    AbstractMenu getMenu(String id);

    /**
     * Открывает указанное меню для игрока, отображая статические и динамические предметы.
     *
     */
    CompletableFuture<Void> open(AbstractMenu abstractMenu, PlayerSnapshot snapshot);

    CompletableFuture<Void> open(AbstractMenu abstractMenu, Player player);

    /**
     * Открывает зарегистрированное меню по его id.
     * Выбрасывает {@link IllegalArgumentException}, если меню с таким id не найдено.
     *
     */
    CompletableFuture<Void> open(String menuId, PlayerSnapshot snapshot);

    /**
     * Закрывает текущее открытое меню игрока, если таковое имеется.
     *
     */
    CompletableFuture<Void> close(PlayerSnapshot snapshot);

    /**
     * Возвращает меню, открытое в данный момент для этого игрока, если таковое имеется.
     */
    CompletableFuture<Optional<AbstractMenu>> activeMenu(PlayerSnapshot snapshot);

    /**
     * Повторно вычисляет {@link AbstractMenu#dynamicItems(PlayerSnapshot)} для текущего
     * открытого меню игрока и обновляет его на месте (например, после изменения баланса).
     * Не выполняет никаких действий, если у игрока не открыто ни одно меню.
     *
     */
    CompletableFuture<Void> refresh(PlayerSnapshot snapshot);
}