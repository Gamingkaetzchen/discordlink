package de.gamingkaetzchen.synccord.discord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

public class PlayerListUpdater {

    private static String channelId;
    private static String messageId;

    private static final String STATE_FILE = "playerlist-state.yml";

    private PlayerListUpdater() {
        // static utility
    }

    /**
     * Beim Plugin-Start aufrufen, um gespeicherte Message/Channel-IDs zu laden.
     * (Optional, aber nice für Restarts)
     */
    public static void init() {
        loadState();
    }

    /**
     * Wird von /setup playerlist aufgerufen, wenn das Embed einmalig gesendet
     * wurde.
     */
    public static void registerBaseMessage(Message message) {
        if (message == null || message.getChannel() == null) {
            return;
        }
        channelId = message.getChannel().getId();
        messageId = message.getId();
        saveState();

        debug("debug_playerlist_base_saved",
                "%channel%", channelId,
                "%message%", messageId
        );

        // Direkt beim Setup einmal aktualisieren
        refreshNow();
    }

    /**
     * Wird vom PlayerActivityListener bei Join/Leave aufgerufen. Baut das
     * Playerlist-Embed neu und editiert die gespeicherte Nachricht.
     */
    public static void refreshNow() {
        Synccord plugin = Synccord.getInstance();

        if (!plugin.getConfig().getBoolean("playerlist.enabled", true)) {
            debug("debug_playerlist_disabled");
            return;
        }

        if (channelId == null || messageId == null) {
            debug("debug_playerlist_no_state");
            return;
        }

        DiscordBot bot = plugin.getDiscordBot();
        if (bot == null) {
            debug("debug_playerlist_bot_null");
            return;
        }

        JDA jda = bot.getJDA();
        if (jda == null) {
            debug("debug_playerlist_jda_null");
            return;
        }

        MessageChannel channel = jda.getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            debug("debug_playerlist_channel_not_found", "%id%", channelId);
            return;
        }

        // Embed zusammenbauen
        EmbedBuilder embed = buildPlayerlistEmbed();

        // Nachricht bearbeiten
        channel.retrieveMessageById(messageId).queue(msg -> {
            msg.editMessageEmbeds(embed.build()).queue();
            debug("debug_playerlist_updated");
        }, failure -> {
            debug("debug_playerlist_edit_failed", "%error%", failure.getMessage());
        });
    }

    // ===================== INTERNES RENDERING =====================
    private static EmbedBuilder buildPlayerlistEmbed() {
        Synccord plugin = Synccord.getInstance();
        var cfg = plugin.getConfig();

        boolean showRank = cfg.getBoolean("playerlist.show-rank", false);
        boolean showAlias = cfg.getBoolean("playerlist.show-alias", true);
        boolean showName = cfg.getBoolean("playerlist.show-name", true);

        String format = cfg.getString("playerlist.format", "{alias} {name}").trim();
        String title = cfg.getString("playerlist.title", Lang.get("setup_playerlist_title"));
        String descTemplate = cfg.getString("playerlist.description",
                Lang.get("setup_playerlist_description"));
        String emptyText = cfg.getString("playerlist.empty", Lang.get("playerlist_empty"));

        List<String> lines = new ArrayList<>();

        LuckPerms lp = plugin.getLuckPerms();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String rank = "";
            String alias = "";
            String name = p.getName();

            String primaryGroup = null;
            if (lp != null) {
                User user = lp.getUserManager().getUser(p.getUniqueId());
                if (user != null) {
                    primaryGroup = user.getPrimaryGroup();
                    rank = primaryGroup != null ? primaryGroup : "";
                }
            }

            if (primaryGroup != null) {
                alias = cfg.getString("playerlist.rank-aliases." + primaryGroup, "");
            }

            String line = format;
            line = line.replace("{rank}", showRank ? rank : "");
            line = line.replace("{alias}", showAlias ? alias : "");
            line = line.replace("{name}", showName ? name : "");

            line = line.trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }

        int count = lines.size();
        String desc = descTemplate.replace("{count}", String.valueOf(count));

        if (count == 0) {
            desc = desc + "\n\n" + emptyText;
        } else {
            StringBuilder sb = new StringBuilder(desc);
            sb.append("\n\n");
            for (String l : lines) {
                sb.append("• ").append(l).append("\n");
            }
            desc = sb.toString().trim();
        }

        String guildId = plugin.getConfig().getString("discord.guild-id");
        String iconUrl = null;
        if (guildId != null && !guildId.isBlank() && plugin.getDiscordBot() != null) {
            var guild = plugin.getDiscordBot().getJDA().getGuildById(guildId);
            if (guild != null) {
                iconUrl = guild.getIconUrl();
            }
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(title)
                .setDescription(desc);

        if (iconUrl != null) {
            eb.setThumbnail(iconUrl);
            eb.setFooter(Lang.get("playerlist_footer"), iconUrl);
        } else {
            eb.setFooter(Lang.get("playerlist_footer"));
        }

        return eb;
    }

    // ===================== STATE SPEICHERN/LADEN =====================
    private static void loadState() {
        Synccord plugin = Synccord.getInstance();
        File file = new File(plugin.getDataFolder(), STATE_FILE);
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        channelId = yml.getString("channel-id");
        messageId = yml.getString("message-id");

        debug("debug_playerlist_state_loaded",
                "%channel%", channelId != null ? channelId : "null",
                "%message%", messageId != null ? messageId : "null"
        );
    }

    private static void saveState() {
        Synccord plugin = Synccord.getInstance();
        File file = new File(plugin.getDataFolder(), STATE_FILE);
        YamlConfiguration yml = new YamlConfiguration();

        yml.set("channel-id", channelId);
        yml.set("message-id", messageId);

        try {
            yml.save(file);
            debug("debug_playerlist_state_saved");
        } catch (IOException e) {
            debug("debug_playerlist_state_save_failed");
            plugin.getLogger().warning("[Synccord] Could not save playerlist-state.yml");
            e.printStackTrace();
        }
    }

    // ===================== DEBUG-HELPER =====================
    private static boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }

    private static void debug(String key) {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(Lang.get(key));
        }
    }

    private static void debug(String key, String placeholder, String value) {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get(key).replace(placeholder, value)
            );
        }
    }

    private static void debug(String key, String p1, String v1, String p2, String v2) {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get(key)
                            .replace(p1, v1)
                            .replace(p2, v2)
            );
        }
    }
}
