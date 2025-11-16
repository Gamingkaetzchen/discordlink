package de.gamingkaetzchen.synccord.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.DiscordBot;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class MinecraftChatListener implements Listener {

    private final Synccord plugin;

    public MinecraftChatListener() {
        this.plugin = Synccord.getInstance();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        // Channel-ID aus der config
        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId == null || channelId.isEmpty()) {
            return;
        }

        DiscordBot bot = plugin.getDiscordBot();
        if (bot == null || bot.getJDA() == null) {
            return;
        }

        TextChannel channel = bot.getJDA().getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        String template = plugin.getConfig().getString(
                "chat.format-to-discord",
                "<%player%> %message%"
        );

        String content = template
                .replace("%player%", event.getPlayer().getName())
                .replace("%message%", event.getMessage());

        // aus Async-Thread raus? JDA ist threadsafe genug, aber wir bleiben sauber:
        Bukkit.getScheduler().runTaskAsynchronously(plugin, ()
                -> channel.sendMessage(content).queue()
        );
    }
}
