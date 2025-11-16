package de.gamingkaetzchen.synccord.discord;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

/**
 * Baut ein hübsches Playerlist-Embed und hält es via Task aktuell.
 *
 * Konfiguration (config.yml):
 *
 * playerlist: enabled: true update-interval-seconds: 30
 *
 * display: luckperms-rank: false config-rank-alias: true player-name: true
 *
 * rank-alias: default: "Spieler" vip: "VIP" mod: "Moderator" admin: "Admin"
 */
public class PlayerListUpdater {

    private static MessageChannel lastChannel;
    private static Message lastMessage;
    private static BukkitTask task;

    private PlayerListUpdater() {
        // Utility-Klasse
    }

    /**
     * Baut das Playerlist-Embed anhand der aktuellen Online-Spieler.
     *
     * @param guildIconUrl Icon der Guild (kann null sein)
     */
    public static EmbedBuilder buildPlayerListEmbed(String guildIconUrl) {
        Synccord plugin = Synccord.getInstance();

        boolean showLpRank = plugin.getConfig().getBoolean("playerlist.display.luckperms-rank", false);
        boolean showAlias = plugin.getConfig().getBoolean("playerlist.display.config-rank-alias", true);
        boolean showName = plugin.getConfig().getBoolean("playerlist.display.player-name", true);

        // Aliase aus Config
        Map<String, Object> aliasSection = plugin.getConfig()
                .getConfigurationSection("playerlist.rank-alias") != null
                ? plugin.getConfig().getConfigurationSection("playerlist.rank-alias").getValues(false)
                : null;

        String defaultAlias = plugin.getConfig().getString("playerlist.rank-alias.default", "Spieler");

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        StringBuilder sb = new StringBuilder();

        if (online.isEmpty()) {
            sb.append(Lang.get("playerlist_empty"));
        } else {
            LuckPerms lp = getLuckPermsSafe();

            for (Player p : online) {
                List<String> parts = new ArrayList<>();

                String primaryGroup = null;
                if (showLpRank && lp != null) {
                    try {
                        User user = lp.getPlayerAdapter(Player.class).getUser(p);
                        if (user != null) {
                            primaryGroup = user.getPrimaryGroup();
                            if (primaryGroup != null && !primaryGroup.isEmpty()) {
                                parts.add(primaryGroup);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (showAlias) {
                    String key = primaryGroup != null ? primaryGroup : "default";
                    String alias = defaultAlias;

                    if (aliasSection != null && aliasSection.containsKey(key)) {
                        Object v = aliasSection.get(key);
                        if (v != null) {
                            alias = v.toString();
                        }
                    }

                    if (alias != null && !alias.isEmpty()) {
                        parts.add(alias);
                    }
                }

                if (showName || parts.isEmpty()) {
                    parts.add(p.getName());
                }

                sb.append("• ")
                        .append(String.join(" ", parts))
                        .append("\n");
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(Lang.get("setup_playerlist_title"))
                .setDescription(sb.toString().trim())
                .setColor(Color.CYAN);

        if (guildIconUrl != null) {
            embed.setThumbnail(guildIconUrl);
            embed.setFooter(Lang.get("playerlist_footer"), guildIconUrl);
        } else {
            embed.setFooter(Lang.get("playerlist_footer"));
        }

        return embed;
    }

    /**
     * Startet oder ersetzt das Auto-Update für das übergebene Embed.
     */
    public static synchronized void startAutoUpdate(MessageChannel channel, Message message) {
        Synccord plugin = Synccord.getInstance();

        if (!plugin.getConfig().getBoolean("playerlist.enabled", true)) {
            return;
        }

        lastChannel = channel;
        lastMessage = message;

        if (task != null) {
            task.cancel();
            task = null;
        }

        long intervalSec = plugin.getConfig().getLong("playerlist.update-interval-seconds", 30L);
        if (intervalSec < 5L) {
            intervalSec = 5L;
        }

        long ticks = intervalSec * 20L;

        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> {
                    try {
                        refreshInternal(null);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("[PlayerListUpdater] Fehler beim Auto-Update: " + ex.getMessage());
                    }
                },
                ticks,
                ticks
        );

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(
                    Lang.get("debug_setup_playerlist_started")
                            .replace("%channel%", channel.getId())
            );
        }
    }

    /**
     * Wird z.B. von Join/Quit-Listener aufgerufen.
     */
    public static void refreshNow() {
        refreshInternal(null);
    }

    /**
     * Optionale Variante, falls man ein Icon mitgeben möchte.
     */
    public static void refreshNow(String guildIconUrl) {
        refreshInternal(guildIconUrl);
    }

    private static synchronized void refreshInternal(String guildIconUrlOverride) {
        if (lastChannel == null || lastMessage == null) {
            return;
        }

        Synccord plugin = Synccord.getInstance();
        if (!plugin.getConfig().getBoolean("playerlist.enabled", true)) {
            return;
        }

        String icon = guildIconUrlOverride;

        try {
            if (icon == null && lastMessage.getGuild() != null) {
                icon = lastMessage.getGuild().getIconUrl();
            }
        } catch (Exception ignored) {
        }

        EmbedBuilder embed = buildPlayerListEmbed(icon);
        lastMessage.editMessageEmbeds(embed.build()).queue();
    }

    private static LuckPerms getLuckPermsSafe() {
        try {
            return LuckPermsProvider.get();
        } catch (IllegalStateException ex) {
            return null;
        }
    }
}
