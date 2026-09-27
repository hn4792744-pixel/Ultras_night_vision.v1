package com.ultras.nightvision.message;

import com.ultras.nightvision.UltrasNightVisionPlugin;
import com.ultras.nightvision.config.ConfigService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Renders MiniMessage text (gradients, colors) and drives the "keep the action bar visible for N seconds"
 *  behaviour by resending it on a short repeating task per player. */
public final class MessageService {
    private final UltrasNightVisionPlugin plugin;
    private final ConfigService config;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, BukkitTask> actionBarTasks = new ConcurrentHashMap<>();

    public MessageService(UltrasNightVisionPlugin plugin, ConfigService config) {
        this.plugin = plugin;
        this.config = config;
    }

    private Component render(String template, Map<String, String> placeholders) {
        String text = template.replace("%prefix%", config.prefix());
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                text = text.replace("%" + e.getKey() + "%", mm.escapeTags(e.getValue()));
            }
        }
        return mm.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public void send(CommandSender to, String key) {
        to.sendMessage(render(config.message(key), null));
    }

    public void send(CommandSender to, String key, String placeholderName, String value) {
        Map<String, String> ph = new HashMap<>();
        ph.put(placeholderName, value);
        to.sendMessage(render(config.message(key), ph));
    }

    /** Sends the action-bar status text and keeps it visible for action-bar.duration-seconds by resending it. */
    public void showPersistentActionBar(Player player, boolean on) {
        if (!config.actionBarEnabled()) return;
        Component text = render(config.message(on ? "actionbar-on" : "actionbar-off"), null);
        cancelActionBar(player.getUniqueId());

        long intervalTicks = config.actionBarResendIntervalTicks();
        long totalTicks = config.actionBarDurationSeconds() * 20L;
        long[] elapsed = {0};

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || elapsed[0] > totalTicks) {
                cancelActionBar(player.getUniqueId());
                return;
            }
            player.sendActionBar(text);
            elapsed[0] += intervalTicks;
        }, 0L, intervalTicks);
        actionBarTasks.put(player.getUniqueId(), task);
    }

    public void cancelActionBar(UUID uuid) {
        BukkitTask task = actionBarTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    /** Call on plugin disable to avoid leaking scheduled tasks. */
    public void cancelAll() {
        actionBarTasks.values().forEach(BukkitTask::cancel);
        actionBarTasks.clear();
    }
}
