// PPFSS_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.item;

import com.ontadev.libs.menu.enums.InteractionType;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.util.interfaces.Copyable;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ItemModel implements Copyable<ItemModel> {

    /**
     * Готовый ItemStack для использования вместо создания нового из {@link #material}.
     * При установке {@link #toItemStack()} клонирует этот стек и применяет поверх него
     * любые ненулевые переопределения.
     */
    private ItemStack baseItem;

    @Builder.Default
    private Material material = Material.STONE;

    private Message name;

    private Message lore;

    @Builder.Default
    private int amount = 1;

    private Integer customModelData;

    @Builder.Default
    private Set<ItemFlag> itemFlags = EnumSet.noneOf(ItemFlag.class);

    /**
     * Должны ли обработчики вызывать {@code event.setCancelled(true)} перед отправкой.
     * По умолчанию true, чтобы предметы меню нельзя было убрать/переместить,
     * если обработчик явно не сделает иначе.
     */
    @Builder.Default
    private boolean cancelClick = true;

    @Builder.Default
    private Map<InteractionType, BiConsumer<Player, InventoryInteractEvent>> interactionHandlers =
            new EnumMap<>(InteractionType.class);

    /** Вызывается, когда нет зарегистрированного обработчика для {@link InteractionType}. */
    private BiConsumer<Player, InventoryInteractEvent> fallbackInteraction;

    /** Оборачивает уже готовый {@link ItemStack} в предмет меню, без применения переопределений метаданных. */
    public static ItemModel of(ItemStack item) {
        return ItemModel.builder().baseItem(item).build();
    }

    /** Регистрирует обработчик для одного типа взаимодействия. Возвращает {@code this} для цепочки вызовов. */
    public ItemModel onInteraction(InteractionType type, BiConsumer<Player, InventoryInteractEvent> handler) {
        ensureHandlerMap();
        interactionHandlers.put(type, handler);
        return this;
    }

    /** Регистрирует один обработчик для нескольких типов, например {@code InteractionType.SHIFT_CLICKS}. */
    public ItemModel onInteraction(BiConsumer<Player, InventoryInteractEvent> handler, InteractionType... types) {
        ensureHandlerMap();
        for (InteractionType type : types) {
            interactionHandlers.put(type, handler);
        }
        return this;
    }

    /** Регистрирует fallback-обработчик для любого взаимодействия без специального обработчика. */
    public ItemModel onAnyInteraction(BiConsumer<Player, InventoryInteractEvent> handler) {
        this.fallbackInteraction = handler;
        return this;
    }

    /**
     * Отправляет {@link InventoryClickEvent} (левый/правый/шифт/бросок/двойной клик и т.д.)
     * соответствующему обработчику. Вызывайте это из вашего слушателя кликов.
     */
    public void handleClick(InventoryClickEvent event) {
        dispatch(InteractionType.fromClickType(event.getClick()), event);
    }

    /**
     * Отправляет {@link InventoryDragEvent} (предмет перетаскивается через несколько слотов)
     * обработчику {@link InteractionType#DRAG}, если он зарегистрирован.
     * Вызывайте это из вашего слушателя перетаскивания для слотов, содержащих этот предмет.
     */
    public void handleDrag(InventoryDragEvent event) {
        dispatch(InteractionType.DRAG, event);
    }

    private void dispatch(InteractionType type, InventoryInteractEvent event) {
        if (cancelClick) {
            event.setCancelled(true);
        }

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        BiConsumer<Player, InventoryInteractEvent> handler =
                (interactionHandlers != null) ? interactionHandlers.get(type) : null;

        if (handler == null) {
            handler = fallbackInteraction;
        }

        if (handler != null) {
            handler.accept(player, event);
        }
    }

    public ItemStack toItemStack() {
        ItemStack item = (baseItem != null)
                ? baseItem.clone()
                : new ItemStack(material != null ? material : Material.STONE, clampAmount(amount));

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (itemFlags != null && !itemFlags.isEmpty()) {
            meta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));
        }

        if (name != null && !name.getComponents().isEmpty()) {
            meta.displayName(name.getComponents().getFirst());
        }

        if (lore != null && !lore.getComponents().isEmpty()) {
            meta.lore(lore.getComponents());
        }

        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        item.setItemMeta(meta);

        if (baseItem == null) {
            item.setAmount(clampAmount(amount));
        }

        return item;
    }

    private void ensureHandlerMap() {
        if (interactionHandlers == null) {
            interactionHandlers = new EnumMap<>(InteractionType.class);
        }
    }

    private static int clampAmount(int amount) {
        return Math.max(Math.min(amount, 64), 1);
    }

    public ItemModel copy() {
        ItemModel copy = this.toBuilder().build();

        copy.baseItem = baseItem == null ? null : baseItem.clone();
        copy.itemFlags = EnumSet.copyOf(itemFlags);
        copy.interactionHandlers = new EnumMap<>(interactionHandlers);

        return copy;
    }
}