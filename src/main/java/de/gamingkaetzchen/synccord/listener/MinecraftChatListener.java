package de.gamingkaetzchen.synccord.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.DiscordBot;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class MinecraftChatListener implements Listener {

    private final Synccord plugin;

    public MinecraftChatListener() {
        this.plugin = Synccord.getInstance();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

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
        // Format laden → Config (priorität) → Lang (fallback)
        // ==============================
        String format = plugin.getConfig().getString(
                "chat.format-to-discord",
                Lang.get("chat_format_discord") // z.B. "<%player%> %message%"
        );

        String content = format
                .replace("%player%", event.getPlayer().getName())
                .replace("%message%", event.getMessage());

        // ==============================
        // Nachricht senden (async)
        // ==============================
        Bukkit.getScheduler().runTaskAsynchronously(plugin, ()
                -> channel.sendMessage(content).queue(
                        success -> debug("debug_chat_forwarded", "%player%", event.getPlayer().getName()),
                        error -> debug("debug_chat_failed", "%error%", error.getMessage())
                )
        );
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
