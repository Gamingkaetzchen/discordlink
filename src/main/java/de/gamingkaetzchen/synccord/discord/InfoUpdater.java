package de.gamingkaetzchen.synccord.discord;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.Instant;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class InfoUpdater {

    private static Message lastMessage;
    private static String lastChannelId;
    private static String lastMessageId;
    private static BukkitRunnable task;

    private static final String STATE_FILE = "status-message.yml";

    public static void startAutoUpdate(MessageChannel channel, Message message) {
        debug("debug_info_starting_autoupdate");
        lastMessage = message;
        lastChannelId = channel.getId();
        lastMessageId = message.getId();
        saveState();
        startTask();
    }

    public static void recoverOrOffline(JDA jda) {
        debug("debug_info_try_recover");
        loadState();

        if (lastChannelId == null || lastMessageId == null) {
            debug("debug_info_no_state_found");
            return;
        }

        var channel = jda.getChannelById(MessageChannel.class, lastChannelId);
        if (channel == null) {
            debug("debug_info_channel_not_found");
            return;
        }

        channel.retrieveMessageById(lastMessageId).queue(
                msg -> {
                    debug("debug_info_message_found");
                    lastMessage = msg;
                    startTask();
                },
                failure -> {
                    debug("debug_info_message_not_found_creating_new");
                    EmbedBuilder embed = buildStatusEmbed(Synccord.getInstance().getDiscordBot().getJDA().getSelfUser().getAvatarUrl());
                    channel.sendMessageEmbeds(embed.build()).setActionRow(
                            Button.primary("show_players", "🔍 " + Lang.get("show_players_button"))
                    ).queue(sentMsg -> {
                        debug("debug_info_message_sent");
                        startAutoUpdate(channel, sentMsg);
                    });
                }
        );
    }

    private static void startTask() {
        if (task != null) {
            task.cancel();
        }

        int interval = Synccord.getInstance().getConfig().getInt("tps-monitor.update-interval", 60);
        debug("debug_info_update_interval", "%interval%", String.valueOf(interval));

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!Synccord.getInstance().getConfig().getBoolean("tps-monitor.auto-update", true)) {
                    debug("debug_info_autoupdate_disabled");
                    cancel();
                    return;
                }

                if (lastMessage == null) {
                    debug("debug_info_message_missing");
                    cancel();
                    return;
                }

                try {
                    String iconUrl = Synccord.getInstance().getDiscordBot().getJDA().getSelfUser().getAvatarUrl();
                    EmbedBuilder embed = buildStatusEmbed(iconUrl);

                    lastMessage.editMessageEmbeds(embed.build()).setActionRow(
                            Button.primary("show_players", "🔍 " + Lang.get("show_players_button"))
                    ).queue(
                            success -> debug("debug_info_embed_updated"),
                            error -> {
                                Bukkit.getLogger().warning(Lang.get("info_embed_error"));
                                debug("debug_info_embed_update_error", "%error%", error.getMessage());
                            }
                    );
                } catch (Exception e) {
                    Bukkit.getLogger().warning(Lang.get("debug_info_embed_update_exception"));
                    e.printStackTrace();
                }
            }
        };

        task.runTaskTimerAsynchronously(Synccord.getInstance(), interval * 20L, interval * 20L);
    }

    public static EmbedBuilder buildStatusEmbed(String iconUrl) {
        String javaIp = Synccord.getInstance().getConfig().getString("discord.java-ip", "???");
        String bedrockIp = Synccord.getInstance().getConfig().getString("discord.bedrock-ip", "???");
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        double[] tps = Bukkit.getServer().getTPS();
        double mspt = Bukkit.getServer().getAverageTickTime();
        String version = Bukkit.getServer().getName() + " " + Bukkit.getMinecraftVersion();

        return new EmbedBuilder()
                .setTitle(Lang.get("info_embed_title"))
                .setDescription(" ")
                .addField(Lang.get("info_embed_field_java"), javaIp, true)
                .addField(Lang.get("info_embed_field_bedrock"), bedrockIp, true)
                .addField(Lang.get("info_embed_field_players"), String.valueOf(onlinePlayers), true)
                .addField(Lang.get("info_embed_field_tps"), String.format("%.2f", tps[0]), true)
                .addField(Lang.get("info_embed_field_mspt"), String.format("%.2f", mspt), true)
                .addField(Lang.get("info_embed_field_version"), version, true)
                .setThumbnail(iconUrl)
                .setFooter(Lang.get("info_embed_footer"), iconUrl)
                .setTimestamp(Instant.now())
                .setColor(Color.CYAN);
    }

    private static void saveState() {
        File file = new File(Synccord.getInstance().getDataFolder(), STATE_FILE);
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("channel-id", lastChannelId);
        cfg.set("message-id", lastMessageId);
        try {
            cfg.save(file);
            debug("debug_info_state_saved");
        } catch (IOException e) {
            Synccord.getInstance().getLogger().warning(Lang.get("debug_info_state_save_failed"));
        }
    }

    private static void loadState() {
        File file = new File(Synccord.getInstance().getDataFolder(), STATE_FILE);
        if (!file.exists()) {
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        lastChannelId = cfg.getString("channel-id");
        lastMessageId = cfg.getString("message-id");
    }

    private static void debug(String key) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info(Lang.get(key));
        }
    }

    private static void debug(String key, String placeholder, String value) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info(Lang.get(key).replace(placeholder, value));
        }
    }

    public static String getLastChannelId() {
        return lastChannelId;
    }

    public static String getLastMessageId() {
        return lastMessageId;
    }
}
