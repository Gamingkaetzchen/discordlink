package de.gamingkaetzchen.synccord.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.PlayerListUpdater;

/**
 * Reagiert auf Join/Quit und aktualisiert sofort die PlayerList-Embed.
 */
public class PlayerActivityListener implements Listener {

    private final Synccord plugin;

    public PlayerActivityListener() {
        this.plugin = Synccord.getInstance();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // minimal delay, damit andere PlayerDaten schon geladen sind
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> PlayerListUpdater.refreshNow(), // Runnable, eindeutig
                10L // ~0,5 Sekunden
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> PlayerListUpdater.refreshNow(), // Runnable, eindeutig
                10L
        );
    }
}
