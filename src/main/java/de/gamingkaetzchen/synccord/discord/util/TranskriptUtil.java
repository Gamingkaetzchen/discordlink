package de.gamingkaetzchen.synccord.discord.util;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TranskriptUtil {

    public static void createAndSendTranscript(JDA jda, MessageChannel channel, String logChannelId) {
        if (!(channel instanceof TextChannel textChannel)) {
            debug("debug_transcript_channel_invalid");
            return;
        }

        textChannel.getHistory().retrievePast(100).queue(messages -> {
            StringBuilder sb = new StringBuilder();
            List<Message> reversed = messages.reversed();
            for (Message msg : reversed) {
                sb.append("[").append(msg.getTimeCreated()).append("] ")
                        .append(msg.getAuthor().getName()).append(": ")
                        .append(msg.getContentDisplay()).append("\n");
            }

            ByteArrayInputStream transcriptStream = new ByteArrayInputStream(
                    sb.toString().getBytes(StandardCharsets.UTF_8)
            );

            TextChannel logChannel = jda.getTextChannelById(logChannelId);
            if (logChannel != null) {
                logChannel.sendFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(transcriptStream, "transcript.txt"))
                        .setEmbeds(new EmbedBuilder()
                                .setTitle(Lang.get("ticket_transcript_title"))
                                .setDescription(Lang.get("ticket_transcript_description"))
                                .setColor(Color.GRAY)
                                .build())
                        .queue();

                debug("debug_transcript_sent".replace("%channel%", logChannel.getName()));
            } else {
                debug("debug_transcript_logchannel_not_found".replace("%id%", logChannelId));
            }
        });
    }

    private static void debug(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("[Debug] " + msg);
        }
    }
}
