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
            sender.sendMessage("§cDu hast keine Berechtigung.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§7Verwendung: /unlinkdiscord <spielername>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null) {
            sender.sendMessage("§cSpieler wurde nie auf dem Server gesehen.");
            return true;
        }

        UUID uuid = target.getUniqueId();

        if (isDebug()) {
            Bukkit.getLogger().info("[Synccord] Unlink-Versuch für UUID: " + uuid + " (" + target.getName() + ")");
        }

        if (!DatabaseManager.isLinked(uuid)) {
            String msg = Lang.get("unlink_not_found").replace("%name%", target.getName());
            sender.sendMessage(msg.replace("&", "§"));
            if (isDebug()) {
                Bukkit.getLogger().info("[Synccord] Keine Verknüpfung für " + uuid + " gefunden.");
            }
            return true;
        }

        DatabaseManager.unlink(uuid);
        String msg = Lang.get("unlink_success").replace("%name%", target.getName());
        sender.sendMessage(msg.replace("&", "§"));

        if (isDebug()) {
            Bukkit.getLogger().info("[Synccord] Discord-Verknüpfung entfernt für " + uuid);
        }

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

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }
}
