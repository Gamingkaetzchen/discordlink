package de.blizzardsmp.synccord.commands;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import de.blizzardsmp.synccord.Synccord;
import de.blizzardsmp.synccord.database.DatabaseManager;
import de.blizzardsmp.synccord.discord.LinkManager;
import de.blizzardsmp.synccord.util.Lang;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

public class DcFindCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("synccord.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Lang.get("dcfind_usage"));
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = player.getUniqueId();

        if (isDebug()) {
            Bukkit.getLogger().info(Lang.get("debug_dcfind_start")
                    .replace("%name%", args[0])
                    .replace("%uuid%", uuid.toString()));
        }

        if (!DatabaseManager.isLinked(uuid)) {
            sender.sendMessage(Lang.get("dcfind_not_found").replace("%name%", args[0]));
            return true;
        }

        Optional<String> discordIdOpt = LinkManager.getDiscordId(uuid);
        if (discordIdOpt.isEmpty()) {
            if (isDebug()) {
                Bukkit.getLogger().info(Lang.get("debug_dcfind_noid").replace("%uuid%", uuid.toString()));
            }
            sender.sendMessage(Lang.get("dcfind_not_found").replace("%name%", args[0]));
            return true;
        }

        String discordId = discordIdOpt.get();
        JDA jda = Synccord.getInstance().getDiscordBot().getJDA();

        if (isDebug()) {
            Bukkit.getLogger().info(Lang.get("debug_dcfind_foundid").replace("%id%", discordId));
        }

        jda.retrieveUserById(discordId).queue(
                (User user) -> {
                    sender.sendMessage(Lang.get("dcfind_success")
                            .replace("%name%", player.getName() != null ? player.getName() : "???")
                            .replace("%id%", discordId)
                            .replace("%tag%", user.getAsTag()));

                    if (isDebug()) {
                        Bukkit.getLogger().info(Lang.get("debug_dcfind_tag").replace("%tag%", user.getAsTag()));
                    }
                },
                (error) -> {
                    sender.sendMessage(Lang.get("dcfind_user_unknown").replace("%id%", discordId));
                }
        );

        return true;
    }

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }
}
