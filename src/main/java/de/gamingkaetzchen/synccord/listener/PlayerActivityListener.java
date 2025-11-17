package de.gamingkaetzchen.synccord.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.PlayerListUpdater;

/**
 * Reagiert auf Join/Quit und aktualisiert die PlayerList-Embed.
 */
public class PlayerActivityListener implements Listener {

    private final Synccord plugin;

    public PlayerActivityListener() {
        this.plugin = Synccord.getInstance();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!isPlayerlistEnabled()) {
            debug("debug_playerlist_disabled_join");
            return;
        }

        debug("debug_playerlist_schedule_join"
                .replace("%player%", event.getPlayer().getName()));

        // kleiner Delay, damit LuckPerms / Daten etc. geladen sind
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                PlayerListUpdater::refreshNow,
                10L // ~0,5 Sekunden
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!isPlayerlistEnabled()) {
            debug("debug_playerlist_disabled_quit");
            return;
        }

        debug("debug_playerlist_schedule_quit"
                .replace("%player%", event.getPlayer().getName()));

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                PlayerListUpdater::refreshNow,
                10L
        );
    }

    private boolean isPlayerlistEnabled() {
        return plugin.getConfig().getBoolean("playerlist.enabled", true);
    }

    private boolean isDebug() {
        return plugin.getConfig().getBoolean("debug", false);
    }

    private void debug(String msg) {
        if (isDebug()) {
            plugin.getLogger().info("[Debug] " + msg);
        }
    }
}
