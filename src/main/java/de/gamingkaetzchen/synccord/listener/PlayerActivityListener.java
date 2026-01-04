package de.gamingkaetzchen.synccord.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.PlayerListUpdater;
import de.gamingkaetzchen.synccord.util.Lang;

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

        debug("debug_playerlist_schedule_join", "%player%", event.getPlayer().getName());

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

        debug("debug_playerlist_schedule_quit", "%player%", event.getPlayer().getName());

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

    private void debug(String key) {
        if (isDebug()) {
            plugin.getLogger().info("🪲 DEBUG | " + Lang.get(key));
        }
    }

    private void debug(String key, String placeholder, String value) {
        if (isDebug()) {
            plugin.getLogger().info(
                    "🪲 DEBUG | " + Lang.get(key).replace(placeholder, value)
            );
        }
    }
}
