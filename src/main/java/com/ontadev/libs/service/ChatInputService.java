// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.service;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.message.MessageParser;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

@SuppressWarnings("unused")
@Service
@AutoListener
public class ChatInputService implements Listener {

    private static final Message DEFAULT_INVALID_NUMBER = new Message("<red>Нужно ввести число. Напиши число ещё раз, либо \"отмена\".");
    private static final String CANCEL_WORD = "отмена";

    private final Plugin plugin;
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

    public ChatInputService(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAwaiting(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public void cancel(Player player) {
        PendingInput removed = pending.remove(player.getUniqueId());
        if (removed != null && removed.onCancel != null) {
            removed.onCancel.run();
        }
    }

    public void requestText(Player player, Message prompt, Consumer<String> onInput) {
        requestText(player, prompt, onInput, null);
    }

    public void requestText(Player player, Message prompt, Consumer<String> onInput, Runnable onCancel) {
        pending.put(player.getUniqueId(), new PendingInput(onInput, onCancel));
        prompt.send(player);
    }

    public void requestNumber(Player player, Message prompt, DoubleConsumer onNumber) {
        requestNumber(player, prompt, DEFAULT_INVALID_NUMBER, onNumber, null);
    }

    public void requestNumber(Player player, Message prompt, Message invalidMessage, DoubleConsumer onNumber, Runnable onCancel) {
        requestText(player, prompt, raw -> {
            try {
                double value = Double.parseDouble(raw.trim().replace(',', '.'));
                onNumber.accept(value);
            } catch (NumberFormatException e) {
                invalidMessage.send(player);
                requestNumber(player, prompt, invalidMessage, onNumber, onCancel);
            }
        }, onCancel);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PendingInput input = pending.get(player.getUniqueId());
        if (input == null) return;

        event.setCancelled(true);
        String message = MessageParser.PLAIN.serialize(event.message());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!pending.remove(player.getUniqueId(), input)) return;

            if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase(CANCEL_WORD)) {
                if (input.onCancel != null) input.onCancel.run();
                return;
            }

            input.onInput.accept(message);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

    private static final class PendingInput {
        private final Consumer<String> onInput;
        private final Runnable onCancel;

        private PendingInput(Consumer<String> onInput, Runnable onCancel) {
            this.onInput = onInput;
            this.onCancel = onCancel;
        }
    }
}
