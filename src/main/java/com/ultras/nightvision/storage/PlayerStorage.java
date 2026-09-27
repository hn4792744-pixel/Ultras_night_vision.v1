package com.ultras.nightvision.storage;

import com.ultras.nightvision.UltrasNightVisionPlugin;
import com.ultras.nightvision.config.ConfigService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Persists the set of players who have Night Vision enabled. Reads happen once at startup (small file, fine
 * on the main thread during onEnable); every write happens on a single dedicated background thread so
 * concurrent toggles never corrupt the file and the main thread is never blocked by disk I/O.
 */
public final class PlayerStorage {
    private final UltrasNightVisionPlugin plugin;
    private final ConfigService config;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UltrasNightVision-Storage");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean writeQueued = new AtomicBoolean(false);

    public PlayerStorage(UltrasNightVisionPlugin plugin, ConfigService config) {
        this.plugin = plugin;
        this.config = config;
    }

    private File file() {
        return new File(plugin.getDataFolder(), config.persistenceFile());
    }

    /** Synchronous on purpose - only called once, during onEnable, before the server accepts players. */
    public Set<UUID> loadBlocking() {
        Set<UUID> out = new java.util.HashSet<>();
        if (!config.persistenceEnabled()) return out;
        File f = file();
        if (!f.exists()) return out;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String raw : y.getStringList("enabled-players")) {
            try {
                out.add(UUID.fromString(raw));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ignoring invalid UUID in " + config.persistenceFile() + ": " + raw);
            }
        }
        return out;
    }

    /** Fire-and-forget async save. Coalesces bursts of toggles into a single write instead of one per toggle. */
    public void saveAsync(Set<UUID> snapshot) {
        if (!config.persistenceEnabled()) return;
        if (!writeQueued.compareAndSet(false, true)) return;
        writer.execute(() -> {
            writeQueued.set(false);
            try {
                YamlConfiguration y = new YamlConfiguration();
                List<String> list = new ArrayList<>();
                for (UUID u : snapshot) list.add(u.toString());
                y.set("enabled-players", list);
                y.save(file());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save " + config.persistenceFile(), e);
            }
        });
    }

    /** Blocking save used only on plugin disable, to guarantee the final state reaches disk. */
    public void saveBlocking(Set<UUID> snapshot) {
        if (!config.persistenceEnabled()) return;
        try {
            YamlConfiguration y = new YamlConfiguration();
            List<String> list = new ArrayList<>();
            for (UUID u : snapshot) list.add(u.toString());
            y.set("enabled-players", list);
            y.save(file());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + config.persistenceFile() + " on shutdown", e);
        }
    }

    public void shutdown() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) writer.shutdownNow();
        } catch (InterruptedException e) {
            writer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
