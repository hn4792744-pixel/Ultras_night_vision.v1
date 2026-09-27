package com.ultras.nightvision;

import com.ultras.nightvision.command.NvCommand;
import com.ultras.nightvision.config.ConfigService;
import com.ultras.nightvision.listener.PlayerConnectionListener;
import com.ultras.nightvision.listener.PotionEffectListener;
import com.ultras.nightvision.listener.RespawnListener;
import com.ultras.nightvision.message.MessageService;
import com.ultras.nightvision.service.NightVisionService;
import com.ultras.nightvision.storage.PlayerStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class UltrasNightVisionPlugin extends JavaPlugin {
    private ConfigService configService;
    private MessageService messageService;
    private NightVisionService nightVisionService;
    private PlayerStorage storage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages/en.yml", false);
        saveResource("messages/ar.yml", false);

        configService = new ConfigService(this);
        configService.load();

        messageService = new MessageService(this, configService);
        storage = new PlayerStorage(this, configService);
        nightVisionService = new NightVisionService(this, configService, messageService, storage);
        nightVisionService.start();

        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(nightVisionService), this);
        getServer().getPluginManager().registerEvents(
                new RespawnListener(nightVisionService), this);
        getServer().getPluginManager().registerEvents(
                new PotionEffectListener(nightVisionService), this);

        NvCommand command = new NvCommand(configService, messageService, nightVisionService, this::reloadAll);
        PluginCommand pluginCommand = getCommand(configService.commandName());
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        } else {
            getLogger().severe("Could not register the '" + configService.commandName()
                    + "' command - check plugin.yml.");
        }

        getLogger().info("Ultras_NightVision enabled.");
    }

    @Override
    public void onDisable() {
        if (messageService != null) messageService.cancelAll();
        if (nightVisionService != null) nightVisionService.stop();
    }

    /** Reloads config.yml + the active language file and restarts the periodic tasks so a changed interval
     *  takes effect immediately. Never touches who currently has Night Vision enabled. */
    private void reloadAll() {
        configService.load();
        nightVisionService.restartTasks();
    }
}
