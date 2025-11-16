package de.gamingkaetzchen.synccord.discord.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import de.gamingkaetzchen.synccord.Synccord;
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

        // Bots ignorieren?
        boolean ignoreBots = plugin.getConfig().getBoolean("chat.ignore-bots", true);
        if (ignoreBots && event.getAuthor().isBot()) {
            return;
        }

        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId == null || channelId.isEmpty()) {
            return;
        }

        // Nur der konfigurierte Chat-Channel
        if (!event.getChannel().getId().equals(channelId)) {
            return;
        }

        String msg = event.getMessage().getContentDisplay();
        if (msg.isEmpty()) {
            return;
        }

        // Optional: Discord-Commands NICHT nach Minecraft schicken
        boolean allowCommands = plugin.getConfig().getBoolean("features.allow-mc-command-to-discord", false);
        if (!allowCommands && msg.startsWith("/")) {
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

        // Auf den Main-Thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(colored);
            }
        });
    }
}
