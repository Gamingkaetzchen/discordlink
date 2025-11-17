package de.gamingkaetzchen.synccord.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public class TicketJoinAlertListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Synccord plugin = Synccord.getInstance();

        if (!player.hasPermission("synccord.ticket.alert")) {
            return;
        }

        // Optional: Feature-Toggle über config
        if (!plugin.getConfig().getBoolean("tickets.alert-on-join", true)) {
            debug("[TicketJoinAlert] alerts disabled in config.");
            return;
        }

        // Bot & JDA prüfen
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJDA() == null) {
            debug("[TicketJoinAlert] DiscordBot/JDA is null, skipping alert check.");
            return;
        }

        String guildId = plugin.getConfig().getString("discord.guild-id");
        if (guildId == null || guildId.isEmpty()) {
            debug("[TicketJoinAlert] No discord.guild-id configured.");
            return;
        }

        // Async → Discord-Abfrage
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            JDA jda = plugin.getDiscordBot().getJDA();
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) {
                debug("[TicketJoinAlert] Guild not found for id=" + guildId);
                return;
            }

            boolean ticketExists = guild.getTextChannels().stream()
                    .anyMatch(c -> c.getName().startsWith("ticket-"));

            if (ticketExists) {
                // Sync → Minecraft-Message
                Bukkit.getScheduler().runTask(plugin, ()
                        -> player.sendMessage(Lang.get("ticket_alert_join"))
                );
            } else {
                debug("[TicketJoinAlert] No open ticket-* channels found.");
            }
        });
    }

    private void debug(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
