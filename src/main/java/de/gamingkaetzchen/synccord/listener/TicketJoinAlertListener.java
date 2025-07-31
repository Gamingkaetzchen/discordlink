package de.gamingkaetzchen.synccord.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.entities.Guild;

public class TicketJoinAlertListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("synccord.ticket.alert")) {
            return;
        }

        // Async → Discord-Abfrage
        Bukkit.getScheduler().runTaskAsynchronously(Synccord.getInstance(), () -> {
            Guild guild = Synccord.getInstance().getDiscordBot().getJDA().getGuildById(
                    Synccord.getInstance().getConfig().getString("discord.guild-id")
            );

            if (guild == null) {
                return;
            }

            boolean ticketExists = guild.getTextChannels().stream()
                    .anyMatch(c -> c.getName().startsWith("ticket-"));

            if (ticketExists) {
                // Sync → Minecraft-Message
                Bukkit.getScheduler().runTask(Synccord.getInstance(), () -> {
                    player.sendMessage(Lang.get("ticket_alert_join"));
                });
            }
        });
    }
}
