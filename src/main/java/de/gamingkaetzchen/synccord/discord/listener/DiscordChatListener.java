package de.gamingkaetzchen.synccord.discord.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordChatListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Nur Guild-Channel, keine DMs
        if (!event.isFromGuild()) {
            return;
        }

        Synccord plugin = Synccord.getInstance();

        debugLog(
                Lang.get("debug_discordchat_received")
                        .replace("%user%", event.getAuthor().getName())
                        .replace("%channel%", event.getChannel().getId())
        );

        // Bots ignorieren?
        boolean ignoreBots = plugin.getConfig().getBoolean("chat.ignore-bots", true);
        if (ignoreBots && event.getAuthor().isBot()) {
            debugLog(
                    Lang.get("debug_discordchat_ignored_bot")
                            .replace("%user%", event.getAuthor().getName())
            );
            return;
        }

        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId == null || channelId.isEmpty()) {
            debugLog(Lang.get("debug_discordchat_no_channel_config"));
            return;
        }

        // Nur der konfigurierte Chat-Channel
        if (!event.getChannel().getId().equals(channelId)) {
            debugLog(
                    Lang.get("debug_discordchat_wrong_channel")
                            .replace("%expected%", channelId)
                            .replace("%actual%", event.getChannel().getId())
            );
            return;
        }

        String msg = event.getMessage().getContentDisplay();
        if (msg.isEmpty()) {
            debugLog(Lang.get("debug_discordchat_empty_message"));
            return;
        }

        // Optional: Discord-Commands NICHT nach Minecraft schicken
        boolean allowCommands = plugin.getConfig().getBoolean("features.allow-mc-command-to-discord", false);
        if (!allowCommands && msg.startsWith("/")) {
            debugLog(
                    Lang.get("debug_discordchat_blocked_command")
                            .replace("%message%", msg)
            );
            return;
        }

        String template = plugin.getConfig().getString(
                "chat.format-to-minecraft",
                "&9[DC] &7%user%: &f%message%"
        );

        String formatted = template
                .replace("%user%", event.getAuthor().getName())
                .replace("%message%", msg);

        String colored = ChatColor.translateAlternateColorCodes('&', formatted);

        debugLog(
                Lang.get("debug_discordchat_broadcast")
                        .replace("%formatted%", colored)
        );

        // Auf den Main-Thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(colored);
            }
        });
    }

    private void debugLog(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
