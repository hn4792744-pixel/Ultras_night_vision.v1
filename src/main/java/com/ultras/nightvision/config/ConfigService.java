package com.ultras.nightvision.config;

import com.ultras.nightvision.UltrasNightVisionPlugin;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;

/** Thin, typed wrapper around config.yml + the selected messages/<lang>.yml. Reload-safe: call load() again
 *  any time (e.g. from /nv reload) and every getter reflects the fresh values immediately. */
public final class ConfigService {
    private final UltrasNightVisionPlugin plugin;
    private volatile YamlConfiguration messages = new YamlConfiguration();

    public ConfigService(UltrasNightVisionPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        String lang = plugin.getConfig().getString("language", "en");
        File file = new File(plugin.getDataFolder(), "messages/" + lang + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("messages/" + lang + ".yml not found - falling back to English.");
            file = new File(plugin.getDataFolder(), "messages/en.yml");
            if (!file.exists()) {
                plugin.saveResource("messages/en.yml", false);
            }
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    private org.bukkit.configuration.file.FileConfiguration cfg() {
        return plugin.getConfig();
    }

    public String message(String key) {
        String v = messages.getString(key);
        return v == null ? key : v;
    }

    public String prefix() {
        return cfg().getString("prefix", "");
    }

    public String commandName() {
        return cfg().getString("command.name", "nv");
    }

    public String permissionUse() {
        return cfg().getString("permissions.use", "ultras.nv.use");
    }

    public String permissionOthers() {
        return cfg().getString("permissions.others", "ultras.nv.others");
    }

    public String permissionAll() {
        return cfg().getString("permissions.all", "ultras.nv.all");
    }

    public String permissionAdmin() {
        return cfg().getString("permissions.admin", "ultras.nv.admin");
    }

    public int cooldownSeconds() {
        return Math.max(0, cfg().getInt("cooldown-seconds", 2));
    }

    public int amplifier() {
        return Math.max(0, cfg().getInt("night-vision.amplifier", 0));
    }

    public int durationTicks() {
        return Math.max(20, cfg().getInt("night-vision.duration-ticks", 999999));
    }

    public int refreshCheckIntervalSeconds() {
        return Math.max(1, cfg().getInt("night-vision.refresh-check-interval-seconds", 15));
    }

    public int reapplyBelowTicks() {
        return Math.max(20, cfg().getInt("night-vision.reapply-below-ticks", 12000));
    }

    public boolean hideParticles() {
        return cfg().getBoolean("night-vision.hide-particles", true);
    }

    public boolean hideIcon() {
        return cfg().getBoolean("night-vision.hide-icon", true);
    }

    public boolean ambient() {
        return cfg().getBoolean("night-vision.ambient", false);
    }

    public boolean persistenceEnabled() {
        return cfg().getBoolean("persistence.enabled", true);
    }

    public String persistenceFile() {
        return cfg().getString("persistence.file", "players.yml");
    }

    public int saveIntervalSeconds() {
        return Math.max(5, cfg().getInt("persistence.save-interval-seconds", 60));
    }

    public boolean actionBarEnabled() {
        return cfg().getBoolean("action-bar.enabled", true);
    }

    public int actionBarDurationSeconds() {
        return Math.max(1, cfg().getInt("action-bar.duration-seconds", 3));
    }

    public int actionBarResendIntervalTicks() {
        return Math.max(5, cfg().getInt("action-bar.resend-interval-ticks", 20));
    }

    public boolean soundsEnabled() {
        return cfg().getBoolean("sounds.enabled", true);
    }

    public String enableSound() {
        return cfg().getString("sounds.enable.sound", "BLOCK_NOTE_BLOCK_CHIME");
    }

    public float enableVolume() {
        return (float) cfg().getDouble("sounds.enable.volume", 0.6);
    }

    public float enablePitch() {
        return (float) cfg().getDouble("sounds.enable.pitch", 1.4);
    }

    public String disableSound() {
        return cfg().getString("sounds.disable.sound", "BLOCK_NOTE_BLOCK_BASS");
    }

    public float disableVolume() {
        return (float) cfg().getDouble("sounds.disable.volume", 0.6);
    }

    public float disablePitch() {
        return (float) cfg().getDouble("sounds.disable.pitch", 0.8);
    }

    public boolean notifyTargetSender() {
        return cfg().getBoolean("target-behavior.notify-sender", true);
    }

    public boolean notifyTargetPlayer() {
        return cfg().getBoolean("target-behavior.notify-target", true);
    }

    public boolean notifyAllSender() {
        return cfg().getBoolean("all-behavior.notify-sender", true);
    }

    public boolean notifyEveryoneOnAll() {
        return cfg().getBoolean("all-behavior.notify-everyone", true);
    }
}
