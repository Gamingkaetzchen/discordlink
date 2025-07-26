package de.gamingkaetzchen.synccord.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.database.DatabaseManager;
import de.gamingkaetzchen.synccord.util.Lang;

public class UnlinkDiscordCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("synccord.admin")) {
            sender.sendMessage(Lang.get("no_permission"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Lang.get("unlink_usage"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null) {
            sender.sendMessage(Lang.get("unlink_never_seen"));
            return true;
        }

        UUID uuid = target.getUniqueId();

        debugLog(Lang.get("debug_unlink_attempt")
                .replace("%name%", target.getName())
                .replace("%uuid%", uuid.toString()));

        if (!DatabaseManager.isLinked(uuid)) {
            sender.sendMessage(Lang.get("unlink_not_found").replace("%name%", target.getName()));
            debugLog(Lang.get("debug_unlink_not_found").replace("%uuid%", uuid.toString()));
            return true;
        }

        DatabaseManager.unlink(uuid);
        sender.sendMessage(Lang.get("unlink_success").replace("%name%", target.getName()));

        debugLog(Lang.get("debug_unlink_success").replace("%uuid%", uuid.toString()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                if (p.getName() != null && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    if (DatabaseManager.isLinked(p.getUniqueId())) {
                        suggestions.add(p.getName());
                    }
                }
            }
            return suggestions;
        }
        return Collections.emptyList();
    }

    private void debugLog(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Bukkit.getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
