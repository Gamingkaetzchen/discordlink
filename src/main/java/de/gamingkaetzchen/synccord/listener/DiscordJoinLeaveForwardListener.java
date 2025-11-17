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
import de.gamingkaetzchen.synccord.util.Lang;

public class DiscordJoinLeaveForwardListener implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Synccord plugin = Synccord.getInstance();

        if (!plugin.getConfig().getBoolean("features.send-join", true)) {
            debug("debug_join_forward_disabled");
            return;
        }

        String channelId = plugin.getConfig().getString("discord.join-channel-id", null);
        if (channelId == null || channelId.isEmpty()) {
            debug("debug_join_channel_missing");
            return;
        }

        List<String> messages = plugin.getConfig().getStringList("discord.join-messages");
        if (messages == null || messages.isEmpty()) {
            debug("debug_join_messages_empty");
            return;
        }

        String raw = messages.get(random.nextInt(messages.size()));
        String msg = applyPlaceholders(
                raw,
                event.getPlayer().getName(),
                plugin.getName(),
                event.getPlayer().getUniqueId().toString(),
                null // kein reason bei Join
        );

        if (plugin.getDiscordBot() == null) {
            debug("debug_join_discordbot_null");
            return;
        }

        plugin.getDiscordBot().sendSimpleEmbed(
                channelId,
                Lang.get("discord_join_title"), // z.B. "🟢 Spieler beigetreten"
                msg,
                Color.decode("#57F287"),
                null
        );

        debug("debug_join_forwarded",
                "%player%", event.getPlayer().getName()
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Synccord plugin = Synccord.getInstance();

        if (!plugin.getConfig().getBoolean("features.send-leave", true)) {
            debug("debug_leave_forward_disabled");
            return;
        }

        String channelId = plugin.getConfig().getString("discord.leave-channel-id", null);
        if (channelId == null || channelId.isEmpty()) {
            debug("debug_leave_channel_missing");
            return;
        }

        List<String> messages = plugin.getConfig().getStringList("discord.leave-messages");
        if (messages == null || messages.isEmpty()) {
            debug("debug_leave_messages_empty");
            return;
        }

        String raw = messages.get(random.nextInt(messages.size()));
        String msg = applyPlaceholders(
                raw,
                event.getPlayer().getName(),
                plugin.getName(),
                event.getPlayer().getUniqueId().toString(),
                null // kein reason bei Leave
        );

        if (plugin.getDiscordBot() == null) {
            debug("debug_leave_discordbot_null");
            return;
        }

        plugin.getDiscordBot().sendSimpleEmbed(
                channelId,
                Lang.get("discord_leave_title"), // z.B. "🔴 Spieler hat verlassen"
                msg,
                Color.decode("#ED4245"),
                null
        );

        debug("debug_leave_forwarded",
                "%player%", event.getPlayer().getName()
        );
    }

    // OPTIONAL: Death forwarden (mit eigener Liste discord.death-messages)
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Synccord plugin = Synccord.getInstance();

        if (!plugin.getConfig().getBoolean("features.send-death", true)) {
            debug("debug_death_forward_disabled");
            return;
        }

        String channelId = plugin.getConfig().getString("discord.chat-channel-id", null);
        if (channelId == null || channelId.isEmpty()) {
            debug("debug_death_channel_missing");
            return;
        }

        List<String> messages = plugin.getConfig().getStringList("discord.death-messages");
        String playerName = event.getEntity().getName();

        // MC-Standardtext, z.B. "Nils wurde von Zombie getötet"
        String deathMessage = event.getDeathMessage();
        if (deathMessage == null || deathMessage.isEmpty()) {
            deathMessage = playerName + " ist gestorben.";
        }

        // Reason ohne Spielernamen (für {reason})
        String reason = deathMessage;
        if (reason.startsWith(playerName)) {
            reason = reason.substring(playerName.length()).trim();
        }

        String raw;
        if (messages != null && !messages.isEmpty()) {
            raw = messages.get(random.nextInt(messages.size()));
        } else {
            // Fallback: einfach den Vanilla-Text schicken
            raw = "{player} ist gestorben: {reason}";
        }

        String msg = applyPlaceholders(
                raw,
                playerName,
                plugin.getName(),
                event.getEntity().getUniqueId().toString(),
                reason
        );

        if (plugin.getDiscordBot() == null) {
            debug("debug_death_discordbot_null");
            return;
        }

        plugin.getDiscordBot().sendSimpleEmbed(
                channelId,
                Lang.get("discord_death_title"), // z.B. "💀 Tod"
                msg,
                Color.GRAY,
                null
        );

        debug("debug_death_forwarded",
                "%player%", playerName
        );
    }

    private String applyPlaceholders(String raw,
            String player,
            String server,
            String uuid,
            String reason) {

        String msg = raw
                .replace("{player}", player)
                .replace("{server}", server)
                .replace("{uuid}", uuid);

        if (reason != null) {
            msg = msg.replace("{reason}", reason);
        }
        return msg;
    }

    // ================= DEBUG-Helper =================
    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }

    private void debug(String key) {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(Lang.get(key));
        }
    }

    private void debug(String key, String placeholder, String value) {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get(key).replace(placeholder, value)
            );
        }
    }
}
