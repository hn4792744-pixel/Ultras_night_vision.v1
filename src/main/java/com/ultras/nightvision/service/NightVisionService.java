package com.ultras.nightvision.service;

import com.ultras.nightvision.UltrasNightVisionPlugin;
import com.ultras.nightvision.config.ConfigService;
import com.ultras.nightvision.message.MessageService;
import com.ultras.nightvision.storage.PlayerStorage;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Single source of truth for who has Night Vision enabled. Handles applying/removing the vanilla potion
 * effect, re-applying it if another plugin (or a milk bucket) strips it, and persisting the toggle set.
 */
public final class NightVisionService {
    private final UltrasNightVisionPlugin plugin;
    private final ConfigService config;
    private final MessageService messages;
    private final PlayerStorage storage;

    /** UUIDs of players who currently have Night Vision enabled (online or not). */
    private final Set<UUID> enabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastToggle = new ConcurrentHashMap<>();
    private BukkitTask refreshTask;
    private BukkitTask saveTask;

    public NightVisionService(UltrasNightVisionPlugin plugin, ConfigService config, MessageService messages,
                              PlayerStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.storage = storage;
    }

    public void start() {
        enabled.addAll(storage.loadBlocking());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (enabled.contains(online.getUniqueId())) applyEffect(online);
        }
        scheduleTasks();
    }

    private void scheduleTasks() {
        int refreshTicks = config.refreshCheckIntervalSeconds() * 20;
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, refreshTicks, refreshTicks);
        int saveTicks = config.saveIntervalSeconds() * 20;
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> storage.saveAsync(Set.copyOf(enabled)),
                saveTicks, saveTicks);
    }

    /** Called after /nv reload so a changed refresh/save interval takes effect without a server restart. */
    public void restartTasks() {
        if (refreshTask != null) refreshTask.cancel();
        if (saveTask != null) saveTask.cancel();
        scheduleTasks();
    }

    public void stop() {
        if (refreshTask != null) refreshTask.cancel();
        if (saveTask != null) saveTask.cancel();
        storage.saveBlocking(Set.copyOf(enabled));
        storage.shutdown();
    }

    public boolean isEnabled(UUID uuid) {
        return enabled.contains(uuid);
    }

    public boolean isOnCooldown(Player player) {
        int cooldown = config.cooldownSeconds();
        if (cooldown <= 0) return false;
        Long last = lastToggle.get(player.getUniqueId());
        return last != null && (System.currentTimeMillis() - last) < cooldown * 1000L;
    }

    /** Toggles one player, applies/removes the effect if they're online, persists the change, plays a sound
     *  and shows the action-bar status. Does NOT send chat messages - callers decide what to say and to whom. */
    public boolean toggle(Player player) {
        lastToggle.put(player.getUniqueId(), System.currentTimeMillis());
        boolean nowEnabled = !enabled.contains(player.getUniqueId());
        setState(player.getUniqueId(), nowEnabled, player);
        return nowEnabled;
    }

    private void setState(UUID uuid, boolean nowEnabled, Player online) {
        if (nowEnabled) enabled.add(uuid); else enabled.remove(uuid);
        storage.saveAsync(Set.copyOf(enabled));
        if (online != null && online.isOnline()) {
            if (nowEnabled) applyEffect(online); else removeEffect(online);
            playToggleSound(online, nowEnabled);
            messages.showPersistentActionBar(online, nowEnabled);
        }
    }

    /** Used by /nv all - flips one player's state without an individual sound/action-bar per player if you
     *  prefer a quieter mass-toggle; here we keep the same per-player feedback for consistency. */
    public boolean toggleSilentSound(Player player, boolean forceState) {
        setState(player.getUniqueId(), forceState, player);
        return forceState;
    }

    private void applyEffect(Player player) {
        PotionEffect current = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
        if (current != null && current.getDuration() >= config.reapplyBelowTicks()
                && current.getAmplifier() == config.amplifier()) {
            return; // already fine, avoid a pointless duplicate application
        }
        PotionEffect effect = new PotionEffect(PotionEffectType.NIGHT_VISION, config.durationTicks(),
                config.amplifier(), config.ambient(), !config.hideParticles(), !config.hideIcon());
        player.addPotionEffect(effect);
    }

    private void removeEffect(Player player) {
        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    /** Called on join and on respawn: vanilla clears effects on respawn, and a player may have been offline
     *  when their toggle changed (e.g. via /nv all), so this brings them back in sync. */
    public void reapplyIfNeeded(Player player) {
        if (enabled.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) applyEffect(player);
            });
        }
    }

    /** Called from the potion-effect listener when something external removes Night Vision from a player who
     *  should still have it. Re-applies once; does not loop, since applyEffect() is itself idempotent. */
    public void handleExternalRemoval(Player player) {
        if (enabled.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) applyEffect(player);
            });
        }
    }

    private void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (enabled.contains(p.getUniqueId())) applyEffect(p);
        }
    }

    private void playToggleSound(Player player, boolean nowEnabled) {
        if (!config.soundsEnabled()) return;
        String name = nowEnabled ? config.enableSound() : config.disableSound();
        float volume = nowEnabled ? config.enableVolume() : config.disableVolume();
        float pitch = nowEnabled ? config.enablePitch() : config.disablePitch();
        try {
            Sound sound = Sound.valueOf(name.toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Unknown sound '" + name + "' in config.yml - skipping.", e);
        }
    }

    /** Cleans in-memory-only state (cooldown + action-bar task) so nothing leaks across long play sessions. */
    public void forget(UUID uuid) {
        lastToggle.remove(uuid);
        messages.cancelActionBar(uuid);
    }
}
