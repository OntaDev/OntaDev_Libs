// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.menu;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.item.ItemModel;
import com.ontadev.libs.menu.manager.MenuManager;
import com.ontadev.libs.menu.task.MenuTask;
import com.ontadev.libs.menu.task.MenuTaskState;
import com.ontadev.libs.player.PlayerResolver;
import com.ontadev.libs.player.PlayerSnapshot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@AutoListener
public class MenuManagerImpl implements MenuManager, Listener {
    @Getter
    private long currentTick = 0L;

    private final PriorityQueue<MenuTaskState> queue = new PriorityQueue<>(Comparator.comparingLong(MenuTaskState::getNextRun));

    /**
     * Внедряется IoC контейнером. Нужен для планирования синхронных задач через {@link Bukkit#getScheduler()}.
     */
    private final Plugin plugin;
    private final PlayerResolver playerResolver;

    private final Map<String, AbstractMenu> menus = new ConcurrentHashMap<>();

    /**
     * Ключ - UUID игрока.
     */
    private final Map<UUID, MenuSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * "Билет" последнего вызова {@link #open}, выданного для каждого игрока.
     * Если за время резолва игрока подоспел более новый open(), старый
     * вызов отменяется внутри своей синхронной задачи.
     */
    private final Map<UUID, Long> openTickets = new ConcurrentHashMap<>();

    public MenuManagerImpl(Plugin plugin, PlayerResolver playerResolver) {
        this.plugin = plugin;
        this.playerResolver = playerResolver;

        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    @Override
    public <T extends AbstractMenu> void registerMenu(T menu) {
        String id = menu.id();
        if (menus.putIfAbsent(id, menu) != null) {
            throw new IllegalStateException("Меню с id '" + id + "' уже зарегистрировано");
        }

        menu.setMenuManager(this);

        runSyncAsync(menu::resolvedStaticItemStacks)
                .exceptionally(ex -> {
                    log.error("Не удалось предварительно построить статичные предметы меню '{}'", id, ex);
                    return null;
                });
    }

    @Override
    public AbstractMenu getMenu(String id) {
        return menus.get(id);
    }

    @Override
    public CompletableFuture<Void> open(AbstractMenu abstractMenu, PlayerSnapshot snapshot) {
        try {
            return playerResolver.getPlayer(snapshot)
                    .thenCompose(player -> {
                        if (player == null || !player.isOnline()) {
                            return CompletableFuture.completedFuture(null);
                        }

                        UUID uuid = player.getUniqueId();
                        long ticket = openTickets.merge(uuid, 1L, Long::sum);

                        return runSyncAsync(() -> {
                            if (!player.isOnline()) return;
                            if (openTickets.get(uuid) != ticket) return;

                            openInternalSync(abstractMenu, player, snapshot);
                        });
                    })
                    .exceptionally(ex -> {
                        log.warn("Не удалось открыть меню '{}' для {}", abstractMenu.id(), snapshot, ex);
                        return null;
                    });
        } catch (Exception ex) {
            log.warn("Не удалось открыть меню '{}' для {}", abstractMenu.id(), snapshot, ex);
            return CompletableFuture.failedFuture(ex);
        }
    }

    @Override
    public CompletableFuture<Void> open(String menuId, PlayerSnapshot snapshot) {
        AbstractMenu abstractMenu = menus.get(menuId);
        if (abstractMenu == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Нет зарегистрированного меню с id '" + menuId + "'"));
        }
        return open(abstractMenu, snapshot);
    }

    @Override
    public CompletableFuture<Void> open(AbstractMenu abstractMenu, Player player) {
        return open(abstractMenu, playerResolver.getPlayerSnapshot(player));
    }

    @Override
    public CompletableFuture<Void> close(PlayerSnapshot snapshot) {
        try {
            return playerResolver.getPlayer(snapshot)
                    .thenCompose(player -> {
                        if (player == null || !player.isOnline()) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return runSyncAsync(player::closeInventory);
                    })
                    .exceptionally(ex -> {
                        log.warn("Не удалось закрыть меню для {}", snapshot, ex);
                        return null;
                    });
        } catch (Exception ex) {
            log.warn("Не удалось закрыть меню для {}", snapshot, ex);
            return CompletableFuture.failedFuture(ex);
        }
    }

    @Override
    public CompletableFuture<Optional<AbstractMenu>> activeMenu(PlayerSnapshot snapshot) {
        return uuidOf(snapshot)
                .thenApply(uuidOpt -> uuidOpt
                        .map(activeSessions::get)
                        .map(MenuSession::getAbstractMenu));
    }

    @Override
    public CompletableFuture<Void> refresh(PlayerSnapshot snapshot) {
        return uuidOf(snapshot)
                .thenCompose(uuidOpt -> {
                    if (uuidOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    UUID uuid = uuidOpt.get();
                    return runSyncAsync(() -> {
                        MenuSession session = activeSessions.get(uuid);
                        if (session != null) {
                            renderSession(session);
                        }
                    });
                })
                .exceptionally(ex -> {
                    log.warn("Не удалось обновить меню для {}", snapshot, ex);
                    return null;
                });
    }

    private void openInternalSync(AbstractMenu abstractMenu, Player player, PlayerSnapshot snapshot) {
        var titleComponent = abstractMenu.title(snapshot).getComponents().getFirst();
        Inventory inventory;

        if (abstractMenu.inventoryType() == null){
            inventory = Bukkit.createInventory(null, abstractMenu.size(), titleComponent);
        }else {
            inventory = Bukkit.createInventory(null, abstractMenu.inventoryType(), titleComponent);
        }

        MenuSession session = new MenuSession(abstractMenu, inventory, abstractMenu.resolveItems(snapshot), snapshot, player);
        renderSession(session);

        activeSessions.put(player.getUniqueId(), session);

        abstractMenu.tasks().forEach(task -> queue.offer(new MenuTaskState(task, session, currentTick)));

        if (abstractMenu.refreshPeriod() > 0) {
            MenuTask task = MenuTask.every(abstractMenu.refreshPeriod(), s -> renderSession(session));
            queue.offer(new MenuTaskState(task, session, currentTick));
        }

        player.openInventory(inventory);
    }

    /**
     * Обновляет предметы в инвентаре сессии на основе динамических данных игрока.
     */
    private void renderSession(MenuSession session) {
        AbstractMenu abstractMenu = session.getAbstractMenu();
        Inventory inventory = session.getInventory();
        PlayerSnapshot snapshot = session.getPlayerSnapshot();

        Map<Integer, ItemModel> items = abstractMenu.resolveItems(snapshot);
        Map<Integer, ItemStack> staticStacks = abstractMenu.resolvedStaticItemStacks();

        items.forEach((slot, model) -> {
            if (slot < 0 || slot >= inventory.getSize()) return;

            ItemStack stack = staticStacks.containsKey(slot)
                    ? staticStacks.get(slot).clone()
                    : model.toItemStack();

            inventory.setItem(slot, stack);
        });

        session.setItems(items);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        MenuSession session = activeSessions.get(event.getWhoClicked().getUniqueId());
        if (session == null || event.getView().getTopInventory() != session.getInventory()) {
            log.warn("session not found");
            return;
        }

        int rawSlot = event.getRawSlot();
        ItemModel model = (rawSlot >= 0 && rawSlot < session.getInventory().getSize())
                ? session.itemAt(rawSlot)
                : null;

        if (model != null) {
            model.handleClick(event);
        } else if (!session.getAbstractMenu().isEditable()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        MenuSession session = activeSessions.get(event.getWhoClicked().getUniqueId());
        if (session == null || event.getView().getTopInventory() != session.getInventory()) {
            return;
        }

        int topSize = session.getInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) continue;

            ItemModel model = session.itemAt(rawSlot);
            if (model != null) {
                model.handleDrag(event);
                return;
            }

            if (!session.getAbstractMenu().isEditable()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        MenuSession session = activeSessions.get(uuid);

        if (session != null && event.getView().getTopInventory() == session.getInventory()) {
            session.setClosed(true);
            activeSessions.remove(uuid);
        }
    }

    /**
     * Защитная очистка на случай, если InventoryCloseEvent не сработал (например, при принудительном отключении).
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        MenuSession session = activeSessions.remove(uuid);
        if (session != null) session.setClosed(true);
        openTickets.remove(uuid);
    }

    /**
     * Асинхронно получает UUID игрока из PlayerSnapshot.
     * Теперь корректно использует PlayerResolver для резолва.
     */
    private CompletableFuture<Optional<UUID>> uuidOf(PlayerSnapshot snapshot) {
        return playerResolver.getPlayer(snapshot)
                .thenApply(player -> {
                    if (player != null && player.isOnline()) {
                        return Optional.of(player.getUniqueId());
                    }
                    return Optional.<UUID>empty();
                })
                .exceptionally(ex -> {
                    log.warn("Не удалось получить UUID для {}", snapshot, ex);
                    return Optional.empty();
                });
    }

    /**
     * Выполняет задачу на главном потоке (синхронно, если уже на нём) и
     * возвращает future, который завершается после её выполнения - либо
     * с ошибкой, если задача бросила исключение или планировщик недоступен
     * (например, плагин уже выключается).
     */
    private CompletableFuture<Void> runSyncAsync(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Runnable wrapped = () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            wrapped.run();
            return future;
        }

        try {
            Bukkit.getScheduler().runTask(plugin, wrapped);
        } catch (IllegalPluginAccessException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private void tick() {
        currentTick++;

        MenuTaskState state;

        while ((state = queue.peek()) != null &&
                state.getNextRun() <= currentTick) {

            queue.poll();

            MenuSession session = state.getSession();

            if (session.isClosed()) {
                continue;
            }

            try {
                state.run(session);
            } catch (Exception ex) {
                log.warn(
                        "Ошибка выполнения MenuTask {}",
                        state.getTask().getClass().getName(),
                        ex
                );
            }

            if (state.getTask().getPeriod() > 0) {
                state.scheduleNext();
                queue.offer(state);
            }
        }
    }
}