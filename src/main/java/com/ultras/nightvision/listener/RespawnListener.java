package com.ultras.nightvision.listener;

import com.ultras.nightvision.service.NightVisionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Vanilla clears every potion effect on respawn - this brings Night Vision straight back for enabled players. */
public final class RespawnListener implements Listener {
    private final NightVisionService nightVision;

    public RespawnListener(NightVisionService nightVision) {
        this.nightVision = nightVision;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        nightVision.reapplyIfNeeded(e.getPlayer());
    }
}
