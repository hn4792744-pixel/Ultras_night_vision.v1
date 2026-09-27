package com.ultras.nightvision.listener;

import com.ultras.nightvision.service.NightVisionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {
    private final NightVisionService nightVision;

    public PlayerConnectionListener(NightVisionService nightVision) {
        this.nightVision = nightVision;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        nightVision.reapplyIfNeeded(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        nightVision.forget(p.getUniqueId());
    }
}
