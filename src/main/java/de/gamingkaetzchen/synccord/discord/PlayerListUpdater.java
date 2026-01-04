package de.gamingkaetzchen.synccord.discord;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

/**
 * Verwaltet die Playerlist-Embed auf Discord. - /setup playerlist erstellt eine
 * Basenachricht und ruft registerBaseMessage(...) - init(JDA) lädt
 * channel/message aus playerlist.yml - refreshNow() aktualisiert die Embed
 * (z.B. bei Join/Quit)
 */
public final class PlayerListUpdater {

    private static final String FILE_NAME = "playerlist.yml";

    private static JDA jda;
    private static String channelId;
    private static String messageId;

    private PlayerListUpdater() {
    }

    public static void init(JDA jdaInstance) {
        jda = jdaInstance;

        Synccord plugin = Synccord.getInstance();
        File file = new File(plugin.getDataFolder(), FILE_NAME);

        if (!file.exists()) {
            debug("debug_playerlist_state_missing");
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        channelId = cfg.getString("channel-id");
        messageId = cfg.getString("message-id");

        debug("debug_playerlist_state_loaded",
                "%channel%", String.valueOf(channelId),
                "%message%", String.valueOf(messageId));
    }

    public static void registerBaseMessage(Message msg) {
        Synccord plugin = Synccord.getInstance();

        channelId = msg.getChannel().getId();
        messageId = msg.getId();

        File file = new File(plugin.getDataFolder(), FILE_NAME);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("channel-id", channelId);
        cfg.set("message-id", messageId);

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe(
                    Lang.get("playerlist_state_save_error")
                            .replace("%error%", e.getMessage() == null ? "null" : e.getMessage())
            );
        }

        debug("debug_playerlist_registered",
                "%channel%", channelId,
                "%message%", messageId);
    }

    public static void refreshNow() {
        Synccord plugin = Synccord.getInstance();

        if (!plugin.getConfig().getBoolean("playerlist.enabled", true)) {
            debug("debug_playerlist_disabled");
            return;
        }

        if (jda == null) {
            debug("debug_playerlist_jda_null");
            return;
        }

        if (channelId == null || messageId == null) {
            debug("debug_playerlist_missing_state");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            debug("debug_playerlist_channel_not_found", "%channel%", channelId);
            return;
        }

        channel.retrieveMessageById(messageId).queue(message -> {
            MessageEmbed embed = buildPlayerListEmbed();

            message.editMessageEmbeds(embed).queue(
                    success -> debug("debug_playerlist_updated"),
                    error -> debug("debug_playerlist_message_edit_failed",
                            "%error%", error.getMessage() == null ? "null" : error.getMessage())
            );
        }, error -> {
            debug("debug_playerlist_message_load_failed",
                    "%error%", error.getMessage() == null ? "null" : error.getMessage());
        });
    }

    /**
     * Baut die Playerlist-Embed: - Titel & Header aus Lang-Keys - Format &
     * Rank-Aliases aus config.yml (playerlist.*)
     */
    private static MessageEmbed buildPlayerListEmbed() {
        Synccord plugin = Synccord.getInstance();
        var config = plugin.getConfig();

        int online = Bukkit.getOnlinePlayers().size();

        // Titel + Header aus Langfile
        String title = Lang.get("playerlist_title");                 // z.B. "👥 Online-Spieler"
        String descriptionPattern = Lang.get("playerlist_header");   // "Aktuell sind {count} Spieler online:"
        String emptyText = Lang.get("playerlist_empty");             // "Niemand ist online 😴"
        String footer = Lang.get("playerlist_footer");               // "Wird automatisch aktualisiert"

        // Format & Aliases aus config
        String format = config.getString("playerlist.format", "{name}");
        boolean showRank = config.getBoolean("playerlist.show-rank", false);
        boolean showAlias = config.getBoolean("playerlist.show-alias", true);
        boolean showName = config.getBoolean("playerlist.show-name", true);
        ConfigurationSection aliasSection = config.getConfigurationSection("playerlist.rank-aliases");

        String header = descriptionPattern.replace("{count}", String.valueOf(online));

        StringBuilder desc = new StringBuilder();
        desc.append(header).append("\n\n");

        if (online == 0) {
            desc.append(emptyText);
        } else {
            LuckPerms luckPerms = plugin.getLuckPerms();

            for (Player p : Bukkit.getOnlinePlayers()) {
                String rank = "";
                String alias = "";

                if (luckPerms != null) {
                    User user = luckPerms.getPlayerAdapter(Player.class).getUser(p);
                    if (user != null) {
                        rank = user.getPrimaryGroup();
                    }
                }

                if (rank == null || rank.isEmpty()) {
                    rank = "default";
                }

                if (aliasSection != null) {
                    alias = aliasSection.getString(rank, rank);
                } else {
                    alias = rank;
                }

                String line = format;
                line = line.replace("{rank}", showRank ? (rank != null ? rank : "") : "");
                line = line.replace("{alias}", showAlias ? (alias != null ? alias : "") : "");
                line = line.replace("{name}", showName ? (p.getName() != null ? p.getName() : "") : "");
                line = line.trim();

                desc.append("• ").append(line).append("\n");
            }
        }

        // Gilden-Icon ermitteln
        String guildIconUrl = null;
        String guildId = config.getString("discord.guild-id");
        Guild guild = null;

        if (guildId != null && !guildId.isEmpty()) {
            guild = jda.getGuildById(guildId);
        }
        if (guild == null && !jda.getGuilds().isEmpty()) {
            guild = jda.getGuilds().get(0);
        }
        if (guild != null) {
            guildIconUrl = guild.getIconUrl();
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(title)
                .setDescription(desc.toString())
                .setColor(java.awt.Color.CYAN);

        if (guildIconUrl != null) {
            eb.setThumbnail(guildIconUrl);
            eb.setFooter(footer, guildIconUrl);
        } else {
            eb.setFooter(footer);
        }

        return eb.build();
    }

    // ===== Debug-Helper =====
    private static boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }

    private static void debug(String key) {
        if (isDebug()) {
            Synccord.getInstance()
                    .getLogger()
                    .info("🪲 DEBUG | " + Lang.get(key));
        }
    }

    private static void debug(String key, String placeholder, String value) {
        if (isDebug()) {
            Synccord.getInstance()
                    .getLogger()
                    .info("🪲 DEBUG | " + Lang.get(key).replace(placeholder, value));
        }
    }

    private static void debug(String key, String p1, String v1, String p2, String v2) {
        if (isDebug()) {
            Synccord.getInstance()
                    .getLogger()
                    .info("🪲 DEBUG | " + Lang.get(key)
                            .replace(p1, v1)
                            .replace(p2, v2));
        }
    }
}
