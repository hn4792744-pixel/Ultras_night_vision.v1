package com.ultras.nightvision.command;

import com.ultras.nightvision.config.ConfigService;
import com.ultras.nightvision.message.MessageService;
import com.ultras.nightvision.service.NightVisionService;
import com.ultras.nightvision.util.ToggleLogic;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /nv, /nv <player>, /nv all, /nv reload (admin). One executor keeps every code path sharing the same
 *  permission / cooldown / protection logic - there is no separate, less-checked path anywhere. */
public final class NvCommand implements TabExecutor {
    private final ConfigService config;
    private final MessageService messages;
    private final NightVisionService nightVision;
    private final Runnable onReload;

    public NvCommand(ConfigService config, MessageService messages, NightVisionService nightVision, Runnable onReload) {
        this.config = config;
        this.messages = messages;
        this.nightVision = nightVision;
        this.onReload = onReload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            return toggleSelf(sender);
        }
        String arg = args[0];
        if (arg.equalsIgnoreCase("reload")) {
            return reload(sender);
        }
        if (arg.equalsIgnoreCase("all")) {
            return toggleAll(sender);
        }
        return toggleTarget(sender, arg);
    }

    private boolean toggleSelf(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission(config.permissionUse())) {
            messages.send(player, "no-permission");
            return true;
        }
        if (nightVision.isOnCooldown(player)) {
            messages.send(player, "cooldown");
            return true;
        }
        boolean nowEnabled = nightVision.toggle(player);
        messages.send(player, nowEnabled ? "enabled-self" : "disabled-self");
        return true;
    }

    private boolean toggleTarget(CommandSender sender, String name) {
        if (!sender.hasPermission(config.permissionOthers())) {
            messages.send(sender, "no-permission");
            return true;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            messages.send(sender, "player-not-found", "player", name);
            return true;
        }
        boolean nowEnabled = nightVision.toggle(target);
        if (config.notifyTargetSender()) {
            messages.send(sender, nowEnabled ? "enabled-target" : "disabled-target", "player", target.getName());
        }
        if (config.notifyTargetPlayer() && !target.equals(sender)) {
            messages.send(target, nowEnabled ? "target-notify-enabled" : "target-notify-disabled");
        }
        return true;
    }

    private boolean toggleAll(CommandSender sender) {
        if (!sender.hasPermission(config.permissionAll())) {
            messages.send(sender, "no-permission");
            return true;
        }
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<Boolean> states = new ArrayList<>();
        for (Player p : online) states.add(nightVision.isEnabled(p.getUniqueId()));
        boolean turnOn = ToggleLogic.decideTurnOn(states);
        for (Player p : online) {
            if (nightVision.isEnabled(p.getUniqueId()) != turnOn) {
                nightVision.toggleSilentSound(p, turnOn);
            }
        }
        if (config.notifyAllSender()) {
            messages.send(sender, turnOn ? "all-enabled" : "all-disabled");
        }
        if (config.notifyEveryoneOnAll()) {
            for (Player p : online) {
                if (!p.equals(sender)) messages.send(p, turnOn ? "all-notify-enabled" : "all-notify-disabled");
            }
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission(config.permissionAdmin())) {
            messages.send(sender, "no-permission");
            return true;
        }
        onReload.run();
        messages.send(sender, "reload-done");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (sender.hasPermission(config.permissionOthers())) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(p.getName());
            }
        }
        if (sender.hasPermission(config.permissionAll()) && "all".startsWith(prefix)) out.add("all");
        if (sender.hasPermission(config.permissionAdmin()) && "reload".startsWith(prefix)) out.add("reload");
        return out;
    }
}
