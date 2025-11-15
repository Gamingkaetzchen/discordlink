package de.gamingkaetzchen.synccord.listener;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.gamingkaetzchen.synccord.Synccord;

public class DiscordJoinLeaveForwardListener implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var plugin = Synccord.getInstance();
        if (!plugin.getConfig().getBoolean("features.send-join", true)) {
            return;
        }

        String channelId = plugin.getConfig().getString("discord.join-channel-id", null);
        if (channelId == null || channelId.isEmpty()) {
            return;
        }

        List<String> messages = plugin.getConfig().getStringList("discord.join-messages");
        if (messages == null || messages.isEmpty()) {
            return;
        }

        String raw = messages.get(random.nextInt(messages.size()));
        String msg = applyPlaceholders(raw, event.getPlayer().getName(), plugin.getName(), event.getPlayer().getUniqueId().toString());

        if (plugin.getDiscordBot() == null) {
            return;
        }
        plugin.getDiscordBot().sendSimpleEmbed(
                channelId,
                "🟢 Spieler beigetreten",
                msg,
                Color.decode("#57F287"),
                null
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var plugin = Synccord.getInstance();
        if (!plugin.getConfig().getBoolean("features.send-leave", true)) {
            return;
        }

        String channelId = plugin.getConfig().getString("discord.leave-channel-id", null);
        if (channelId == null || channelId.isEmpty()) {
            return;
        }

        List<String> messages = plugin.getConfig().getStringList("discord.leave-messages");
        if (messages == null || messages.isEmpty()) {
            return;
        }

        String raw = messages.get(random.nextInt(messages.size()));
        String msg = applyPlaceholders(raw, event.getPlayer().getName(), plugin.getName(), event.getPlayer().getUniqueId().toString());

        if (plugin.getDiscordBot() == null) {
            return;
        }
        plugin.getDiscordBot().sendSimpleEmbed(
                channelId,
                "🔴 Spieler hat verlassen",
                msg,
                Color.decode("#ED4245"),
                null
        );
    }

    // OPTIONAL: death forwarden, weil du es in der config hast
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        var plugin = Synccord.getInstance();
        if (!plugin.getConfig().getBoolean("features.send-death", true)) {
            return;
        }

        String channelId = plugin.getConfig().getString("discord.chat-channel-id", null);
        if (channelId == null || channelId.isEmpty()) {
            return;
        }

        // Standard-MC-Todestext
        String deathMessage = event.getDeathMessage();
        if (deathMessage == null) {
            deathMessage = event.getEntity().getName() + " ist gestorben.";
        }

        if (plugin.getDiscordBot() == null) {
            return;
        }
        plugin.getDiscordBot().sendSimpleEmbed(
                channelId,
                "💀 Tod",
                deathMessage,
                Color.GRAY,
                null
        );
    }

    private String applyPlaceholders(String raw, String player, String server, String uuid) {
        return raw
                .replace("{player}", player)
                .replace("{server}", server)
                .replace("{uuid}", uuid);
    }
}
