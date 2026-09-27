package com.ultras.nightvision.listener;

import com.ultras.nightvision.service.NightVisionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Detects Night Vision being removed by something other than this plugin (a milk bucket, another plugin,
 * a plugin-triggered clear) and re-applies it for players who should still have it. Guards on the change
 * reason so this never fights its own applyEffect() call and never loops.
 */
public final class PotionEffectListener implements Listener {
    private final NightVisionService nightVision;

    public PotionEffectListener(NightVisionService nightVision) {
        this.nightVision = nightVision;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent e) {
        if (e.getModifiedType() != PotionEffectType.NIGHT_VISION) return;
        if (!(e.getEntity() instanceof Player player)) return;
        boolean removed = e.getAction() == EntityPotionEffectEvent.Action.REMOVED
                || e.getAction() == EntityPotionEffectEvent.Action.CLEARED;
        if (!removed) return;
        // A removal caused by this very plugin only happens via NightVisionService.toggle(), which already
        // updates its own state before touching the effect - nothing further to do in that case, and
        // handleExternalRemoval() below is a no-op for players who are no longer supposed to have it.
        nightVision.handleExternalRemoval(player);
    }
}
