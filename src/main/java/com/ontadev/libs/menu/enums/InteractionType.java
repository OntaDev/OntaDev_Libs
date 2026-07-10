// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.menu.enums;

import org.bukkit.event.inventory.ClickType;

public enum InteractionType {

    LEFT_CLICK,
    RIGHT_CLICK,
    SHIFT_LEFT_CLICK,
    SHIFT_RIGHT_CLICK,
    MIDDLE_CLICK,
    DOUBLE_CLICK,
    DROP,
    CONTROL_DROP,
    NUMBER_KEY,
    SWAP_OFFHAND,
    WINDOW_BORDER,

    /** Перетаскивание предмета через несколько слотов (InventoryDragEvent). */
    DRAG,

    /** Всё, что не описано выше (будущие типы кликов и т.д.). */
    UNKNOWN;

    /** Оба варианта с шифтом. */
    public static final InteractionType[] SHIFT_CLICKS = {SHIFT_LEFT_CLICK, SHIFT_RIGHT_CLICK};

    /** Выбрасывание одного предмета или всего стака. */
    public static final InteractionType[] DROP_CLICKS = {DROP, CONTROL_DROP};

    /** Обычный левый/правый клик (без шифта и выбрасывания). */
    public static final InteractionType[] PLAIN_CLICKS = {LEFT_CLICK, RIGHT_CLICK};


    public static InteractionType fromClickType(ClickType clickType) {
        switch (clickType) {
            case LEFT: return LEFT_CLICK;
            case RIGHT: return RIGHT_CLICK;
            case SHIFT_LEFT: return SHIFT_LEFT_CLICK;
            case SHIFT_RIGHT: return SHIFT_RIGHT_CLICK;
            case MIDDLE: return MIDDLE_CLICK;
            case DOUBLE_CLICK: return DOUBLE_CLICK;
            case DROP: return DROP;
            case CONTROL_DROP: return CONTROL_DROP;
            case NUMBER_KEY: return NUMBER_KEY;
            case SWAP_OFFHAND: return SWAP_OFFHAND;
            case WINDOW_BORDER_LEFT:
            case WINDOW_BORDER_RIGHT: return WINDOW_BORDER;
            default: return UNKNOWN;
        }
    }
}