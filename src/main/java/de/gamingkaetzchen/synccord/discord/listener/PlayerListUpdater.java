package de.gamingkaetzchen.synccord.discord.listener;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

public class PlayerListUpdater {

    private static JDA jda;
    private static String channelId;
    private static String messageId;

    private PlayerListUpdater() {
    }

    public static void init(JDA jdaInstance) {
        jda = jdaInstance;

        Synccord plugin = Synccord.getInstance();
        File file = new File(plugin.getDataFolder(), "playerlist_state.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        channelId = cfg.getString("channel-id");
        messageId = cfg.getString("message-id");

        if (plugin.isDebug()) {
            plugin.getLogger().info("[Debug] Playerlist-State geladen: channel=" + channelId + ", message=" + messageId);
        }
    }

    public static void registerBaseMessage(Message message) {
        channelId = message.getChannel().getId();
        messageId = message.getId();

        saveState();

        if (Synccord.getInstance().isDebug()) {
            Synccord.getInstance().getLogger().info("[Debug] Playerlist-Basisnachricht registriert: " + channelId + " / " + messageId);
        }

        refreshNow();
    }

    private static void saveState() {
        Synccord plugin = Synccord.getInstance();
        File file = new File(plugin.getDataFolder(), "playerlist_state.yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("channel-id", channelId);
        cfg.set("message-id", messageId);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[Synccord] Konnte playerlist_state.yml nicht speichern!");
            e.printStackTrace();
        }
    }

    public static void refreshNow() {
        Synccord plugin = Synccord.getInstance();

        if (!plugin.getConfig().getBoolean("playerlist.enabled", false)) {
            return;
        }
        if (jda == null || channelId == null || messageId == null) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        channel.retrieveMessageById(messageId).queue(message -> {
            EmbedBuilder embed = buildPlayerListEmbed();
            message.editMessageEmbeds(embed.build()).queue();
        }, failure -> {
            if (plugin.isDebug()) {
                plugin.getLogger().warning("[Debug] Konnte Playerlist-Message nicht laden: " + failure.getMessage());
            }
        });
    }

    private static EmbedBuilder buildPlayerListEmbed() {
        Synccord plugin = Synccord.getInstance();
        var cfg = plugin.getConfig().getConfigurationSection("playerlist");

        String title = cfg.getString("title", "👥 Online-Spieler");
        String descriptionTemplate = cfg.getString("description", "Aktuell sind {count} Spieler online:");
        String emptyText = cfg.getString("empty", "Niemand ist online 😴");
        String lineFormat = cfg.getString("format", "{alias} {name}");

        boolean showRank = cfg.getBoolean("show-rank", false);
        boolean showAlias = cfg.getBoolean("show-alias", true);
        boolean showName = cfg.getBoolean("show-name", true);

        var aliasSection = cfg.getConfigurationSection("rank-aliases");

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        int count = players.size();

        StringBuilder lines = new StringBuilder();

        if (count == 0) {
            lines.append(emptyText);
        } else {
            LuckPerms lp = plugin.getLuckPerms();

            for (Player p : players) {
                String rank = "";
                String alias = "";
                String name = p.getName();

                if (lp != null) {
                    User user = lp.getUserManager().getUser(p.getUniqueId());
                    if (user != null) {
                        String primaryGroup = user.getPrimaryGroup();
                        rank = primaryGroup;

                        if (aliasSection != null && aliasSection.contains(primaryGroup)) {
                            alias = aliasSection.getString(primaryGroup, "");
                        }
                    }
                }

                String line = lineFormat;
                if (!showRank) {
                    line = line.replace("{rank}", "");
                } else {
                    line = line.replace("{rank}", rank != null ? rank : "");
                }

                if (!showAlias) {
                    line = line.replace("{alias}", "");
                } else {
                    line = line.replace("{alias}", alias != null ? alias : "");
                }

                if (!showName) {
                    line = line.replace("{name}", "");
                } else {
                    line = line.replace("{name}", name != null ? name : "");
                }

                lines.append(line.trim()).append("\n");
            }
        }

        String description = descriptionTemplate.replace("{count}", String.valueOf(count));

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description + "\n\n" + lines.toString().trim());

        String guildIconUrl = null;
        if (jda != null && !jda.getGuilds().isEmpty()) {
            guildIconUrl = jda.getGuilds().get(0).getIconUrl();
        }
        if (guildIconUrl != null) {
            eb.setThumbnail(guildIconUrl);
            eb.setFooter(Lang.get("playerlist_footer"), guildIconUrl);
        } else {
            eb.setFooter(Lang.get("playerlist_footer"), guildIconUrl);
        }

        return eb;
    }
}
