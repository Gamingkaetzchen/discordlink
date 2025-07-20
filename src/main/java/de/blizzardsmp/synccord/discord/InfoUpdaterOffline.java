package de.blizzardsmp.synccord.discord;

import java.awt.Color;
import java.time.Instant;

import de.blizzardsmp.synccord.Synccord;
import de.blizzardsmp.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class InfoUpdaterOffline {

    public static void sendOfflineEmbedSync() {
        String channelId = InfoUpdater.getLastChannelId();
        String messageId = InfoUpdater.getLastMessageId();

        if (channelId == null || messageId == null) {
            debugLog(Lang.get("debug_offline_no_saved_message"));
            return;
        }

        MessageChannel channel = Synccord.getInstance().getDiscordBot().getJDA()
                .getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            debugLog(Lang.get("debug_offline_channel_not_found").replace("%id%", channelId));
            return;
        }

        String iconUrl = Synccord.getInstance().getDiscordBot().getJDA().getSelfUser().getAvatarUrl();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(Lang.get("info_embed_title"))
                .setDescription(Lang.get("info_embed_offline"))
                .setThumbnail(iconUrl)
                .setFooter(Lang.get("info_embed_footer"), iconUrl)
                .setColor(Color.RED)
                .setTimestamp(Instant.now());

        try {
            Message msg = channel.retrieveMessageById(messageId).complete();
            msg.editMessageEmbeds(embed.build())
                    .setActionRow(Button.primary("show_players", "🔍 " + Lang.get("show_players_button")).asDisabled())
                    .complete();
            debugLog(Lang.get("debug_offline_embed_replaced"));
        } catch (Exception e) {
            Synccord.getInstance().getLogger().warning(Lang.get("debug_offline_replace_failed"));
            e.printStackTrace();
        }
    }

    private static void debugLog(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
