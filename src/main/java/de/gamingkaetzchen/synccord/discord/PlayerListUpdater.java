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

        if (Synccord.getInstance().isDebug()) {
            Synccord.getInstance().getLogger().info(
                    "[Debug] Playerlist base message saved: channel=" + channelId + ", message=" + messageId);
        }

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
            if (plugin.isDebug()) {
                plugin.getLogger().info("[Debug] Playerlist disabled in config (playerlist.enabled = false).");
            }
            return;
        }

        if (channelId == null || messageId == null) {
            if (plugin.isDebug()) {
                plugin.getLogger().info("[Debug] No playerlist state found (channelId/messageId is null). "
                        + "Run /setup playerlist first.");
            }
            return;
        }

        DiscordBot bot = plugin.getDiscordBot();
        if (bot == null) {
            if (plugin.isDebug()) {
                plugin.getLogger().info("[Debug] DiscordBot is null – cannot update playerlist.");
            }
            return;
        }

        JDA jda = bot.getJDA();
        if (jda == null) {
            if (plugin.isDebug()) {
                plugin.getLogger().info("[Debug] JDA is null – cannot update playerlist.");
            }
            return;
        }

        MessageChannel channel = jda.getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            if (plugin.isDebug()) {
                plugin.getLogger().info("[Debug] Playerlist channel not found for ID " + channelId);
            }
            return;
        }

        // Embed zusammenbauen
        EmbedBuilder embed = buildPlayerlistEmbed();

        // Nachricht bearbeiten
        channel.retrieveMessageById(messageId).queue(msg -> {
            msg.editMessageEmbeds(embed.build()).queue();

            if (plugin.isDebug()) {
                plugin.getLogger().info("[Debug] Playerlist embed updated.");
            }
        }, failure -> {
            if (plugin.isDebug()) {
                plugin.getLogger().warning("[Debug] Could not edit playerlist message: " + failure.getMessage());
            }
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
        if (guildId != null && !guildId.isBlank()) {
            var guild = Synccord.getInstance().getDiscordBot().getJDA().getGuildById(guildId);
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

        if (plugin.isDebug()) {
            plugin.getLogger().info("[Debug] Loaded playerlist state: channel=" + channelId + ", message=" + messageId);
        }
    }

    private static void saveState() {
        Synccord plugin = Synccord.getInstance();
        File file = new File(plugin.getDataFolder(), STATE_FILE);
        YamlConfiguration yml = new YamlConfiguration();

        yml.set("channel-id", channelId);
        yml.set("message-id", messageId);

        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[Synccord] Could not save playerlist-state.yml");
            e.printStackTrace();
        }
    }
}
