package de.gamingkaetzchen.synccord.listener;

import java.util.Collections;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.DiscordBot;
import de.gamingkaetzchen.synccord.util.Lang;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

public class MinecraftChatListener implements Listener {

    private final Synccord plugin;
    private final boolean papiEnabled;

    public MinecraftChatListener() {
        this.plugin = Synccord.getInstance();
        this.papiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

        // Eventdaten sichern, da wir später im Main-Thread arbeiten
        final Player player = event.getPlayer();
        final String rawMessage = event.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> {

            // ==============================
            // CHANNEL CHECK
            // ==============================
            String channelId = plugin.getConfig().getString("discord.chat-channel-id");
            if (channelId == null || channelId.isEmpty()) {
                debug("debug_chat_channel_missing");
                return;
            }

            DiscordBot bot = plugin.getDiscordBot();
            if (bot == null || bot.getJDA() == null) {
                debug("debug_chat_bot_null");
                return;
            }

            TextChannel channel = bot.getJDA().getTextChannelById(channelId);
            if (channel == null) {
                debug("debug_chat_channel_not_found", "%id%", channelId);
                return;
            }

            // ==============================
            // Format laden → Config (Priorität) → Lang (Fallback)
            // Beispiel in config.yml:
            // chat:
            //   format-to-discord: "<%luckperms_prefix%%player_name%> %message%"
            // ==============================
            String format = plugin.getConfig().getString(
                    "chat.format-to-discord",
                    Lang.get("chat_format_discord") // z.B. "<%player%> %message%"
            );

            String content = format
                    .replace("%player%", player.getName())
                    .replace("%message%", rawMessage);

            // ==============================
            // PlaceholderAPI anwenden (falls vorhanden)
            // ==============================
            if (papiEnabled) {
                try {
                    content = PlaceholderAPI.setPlaceholders(player, content);
                } catch (Exception e) {
                    debug("debug_chat_papi_error", "%error%", e.getMessage());
                }
            }

            // ==============================
            // Pings / Mentions blockieren (optional)
            // ==============================
            boolean blockMentions = plugin.getConfig().getBoolean("chat.block-mentions", true);

            String toSend = content;
            if (blockMentions) {
                // everyone/here "entgiften", damit sie nicht mehr triggern
                toSend = toSend
                        .replace("@everyone", "@\u200Beveryone")
                        .replace("@here", "@\u200Bhere");
            }

            MessageCreateAction action = channel.sendMessage(toSend);

            if (blockMentions) {
                // Verhindert ALLE Mentions (User, Rollen, everyone, here)
                action = action.setAllowedMentions(Collections.emptyList());
                debug("debug_chat_mentions_blocked");
            }

            // ==============================
            // Nachricht an Discord senden (JDA ist selbst asynchron)
            // ==============================
            action.queue(
                    success -> debug("debug_chat_forwarded", "%player%", player.getName()),
                    error -> debug("debug_chat_failed", "%error%", error.getMessage())
            );
        });
    }

    // ==============================
    // Debug Helpers
    // ==============================
    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }

    private void debug(String key) {
        if (isDebug()) {
            plugin.getLogger().info("[Debug] " + Lang.get(key));
        }
    }

    private void debug(String key, String placeholder, String value) {
        if (isDebug()) {
            plugin.getLogger().info("[Debug] " + Lang.get(key).replace(placeholder, value));
        }
    }
}
