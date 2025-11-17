package de.gamingkaetzchen.synccord.commands;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.database.DatabaseManager;
import de.gamingkaetzchen.synccord.discord.LinkManager;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

public class DcFindCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        debugLog(Lang.get("debug_dcfind_command_received")
                .replace("%sender%", sender.getName())
                .replace("%args%", args.length > 0 ? String.join(" ", args) : "(no args)"));

        if (!sender.hasPermission("synccord.admin")) {
            sender.sendMessage(Lang.get("no_permission"));
            debugLog(Lang.get("debug_dcfind_no_permission")
                    .replace("%sender%", sender.getName()));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Lang.get("dcfind_usage"));
            debugLog(Lang.get("debug_dcfind_wrong_usage")
                    .replace("%sender%", sender.getName()));
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = player.getUniqueId();

        debugLog(Lang.get("debug_dcfind_start")
                .replace("%name%", args[0])
                .replace("%uuid%", uuid.toString()));

        if (!DatabaseManager.isLinked(uuid)) {
            sender.sendMessage(Lang.get("dcfind_not_found").replace("%name%", args[0]));
            debugLog(Lang.get("debug_dcfind_not_linked")
                    .replace("%name%", args[0])
                    .replace("%uuid%", uuid.toString()));
            return true;
        }

        Optional<String> discordIdOpt = LinkManager.getDiscordId(uuid);
        if (discordIdOpt.isEmpty()) {
            debugLog(Lang.get("debug_dcfind_noid").replace("%uuid%", uuid.toString()));
            sender.sendMessage(Lang.get("dcfind_not_found").replace("%name%", args[0]));
            return true;
        }

        String discordId = discordIdOpt.get();
        debugLog(Lang.get("debug_dcfind_foundid").replace("%id%", discordId));

        JDA jda = Synccord.getInstance().getDiscordBot().getJDA();

        jda.retrieveUserById(discordId).queue(
                (User user) -> {
                    String playerName = player.getName() != null ? player.getName() : Lang.get("unknown_player_name");

                    sender.sendMessage(Lang.get("dcfind_success")
                            .replace("%name%", playerName)
                            .replace("%id%", discordId)
                            .replace("%tag%", user.getAsTag()));

                    debugLog(Lang.get("debug_dcfind_tag")
                            .replace("%tag%", user.getAsTag())
                            .replace("%name%", playerName)
                            .replace("%id%", discordId));
                },
                (error) -> {
                    sender.sendMessage(Lang.get("dcfind_user_unknown").replace("%id%", discordId));
                    debugLog(Lang.get("debug_dcfind_user_unknown_error")
                            .replace("%id%", discordId)
                            .replace("%error%", error.getMessage() != null ? error.getMessage() : "null"));
                }
        );

        return true;
    }

    private void debugLog(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Bukkit.getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
