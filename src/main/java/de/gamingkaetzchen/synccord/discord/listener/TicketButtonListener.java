package de.gamingkaetzchen.synccord.discord.listener;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.tickets.TicketManager;
import de.gamingkaetzchen.synccord.tickets.TicketQuestion;
import de.gamingkaetzchen.synccord.tickets.TicketType;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu.SelectTarget;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.ArrayList;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TicketButtonListener extends ListenerAdapter {

    private final TicketManager ticketManager;
    private final TicketChannelCreator channelCreator;

    public TicketButtonListener(TicketManager ticketManager) {
        this.ticketManager = ticketManager;
        this.channelCreator = new TicketChannelCreator(ticketManager);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("ticket:add_user")) {
            EntitySelectMenu menu = EntitySelectMenu.create("ticket:add_user_select", SelectTarget.USER)
                    .setPlaceholder("Benutzer auswählen …")
                    .setMinValues(1)
                    .setMaxValues(1)
                    .build();

            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.YELLOW)
                    .setDescription(Lang.get("ticket_add_user_select"))
                    .setFooter("Ticket-System | Benutzer hinzufügen", event.getJDA().getSelfUser().getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now());

            event.replyEmbeds(embed.build()).addActionRow(menu).setEphemeral(true).queue();
            return;
        }

        if (id.equals("ticket:claim")) {
            Member claimer = event.getMember();
            if (claimer == null) {
                replyEmbed(event, Lang.get("ticket_no_user"), Color.RED);
                return;
            }

            MessageChannel channel = event.getChannel();
            channel.getHistory().retrievePast(10).queue(messages -> {
                for (Message message : messages) {
                    if (!message.getEmbeds().isEmpty()) {
                        MessageEmbed oldEmbed = message.getEmbeds().get(0);
                        boolean alreadyClaimed = oldEmbed.getFields().stream()
                                .anyMatch(field -> field.getName().contains("Übernommen von"));

                        if (alreadyClaimed) {
                            replyEmbed(event, Lang.get("ticket_already_claimed"), Color.RED);
                            return;
                        }

                        EmbedBuilder builder = new EmbedBuilder(oldEmbed)
                                .setColor(Color.ORANGE)
                                .addField("🔒 Übernommen von", claimer.getAsMention(), false);

                        message.editMessageEmbeds(builder.build()).queue();
                        channel.sendMessageEmbeds(new EmbedBuilder()
                                .setDescription(Lang.get("ticket_claimed_broadcast").replace("%user%", claimer.getAsMention()))
                                .setColor(Color.ORANGE)
                                .build()).queue();

                        replyEmbed(event, Lang.get("ticket_claimed_success"), Color.GREEN);
                        return;
                    }
                }
                replyEmbed(event, Lang.get("ticket_message_not_found"), Color.RED);
            });
            return;
        }

        if (id.equals("ticket:close")) {
            String channelId = event.getChannel().getId();
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription(Lang.get("ticket_close_confirm"))
                    .setTimestamp(Instant.now());

            event.replyEmbeds(embed.build())
                    .addActionRow(
                            Button.danger("ticket:confirm_close:" + channelId, Lang.get("ticket_close_confirm_yes")),
                            Button.secondary("ticket:cancel_close:" + channelId, Lang.get("ticket_close_confirm_no"))
                    ).setEphemeral(true).queue();
            return;
        }

        if (id.startsWith("ticket:confirm_close:")) {
            String channelId = id.split(":")[2];
            TextChannel ticketChannel = event.getJDA().getTextChannelById(channelId);
            if (ticketChannel == null) {
                replyEmbed(event, Lang.get("ticket_channel_not_found"), Color.RED);
                return;
            }

            File configFile = new File("tickets", channelId + ".yml");
            if (!configFile.exists()) {
                replyEmbed(event, Lang.get("ticket_type_not_found"), Color.RED);
                return;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            String typeId = config.getString("ticket_id");
            TicketType type = Synccord.getInstance().getTicketManager().getTicketTypeById(typeId);

            if (typeId == null || type == null) {
                replyEmbed(event, Lang.get("ticket_type_not_found"), Color.RED);
                return;
            }

            // 🟢 Hole den Log-Channel aus der config
            String logChannelId = Synccord.getInstance().getConfig().getString("tickets." + type.getId() + ".log-channel-id");
            TextChannel logChannel = event.getGuild().getTextChannelById(logChannelId);

            event.deferReply(true).queue();

            ticketChannel.getHistory().retrievePast(100).queue(messages -> {
                StringBuilder transcript = new StringBuilder();

                for (int i = messages.size() - 1; i >= 0; i--) {
                    Message msg = messages.get(i);
                    transcript.append("[").append(msg.getTimeCreated()).append("] ")
                            .append(msg.getAuthor().getName()).append(": ")
                            .append(msg.getContentDisplay()).append("\n");
                }

                try {
                    File dir = new File("ticket", type.getId());
                    dir.mkdirs();
                    File file = new File(dir, channelId + ".txt");
                    Files.write(file.toPath(), transcript.toString().getBytes(StandardCharsets.UTF_8));

                    // 📤 Sende Transkript direkt in den Log-Channel (falls vorhanden)
                    if (logChannel != null) {
                        ByteArrayInputStream input = new ByteArrayInputStream(Files.readAllBytes(file.toPath()));
                        FileUpload upload = FileUpload.fromData(input, "transcript.txt");

                        EmbedBuilder embed = new EmbedBuilder()
                                .setTitle("📁 Transkript für Ticket")
                                .setDescription("Ticket `" + ticketChannel.getName() + "` wurde geschlossen.")
                                .setColor(Color.DARK_GRAY)
                                .setTimestamp(Instant.now());

                        logChannel.sendMessageEmbeds(embed.build())
                                .addFiles(upload)
                                .queue(msg -> {
                                    msg.delete().queueAfter(5, TimeUnit.MINUTES);
                                    file.delete();
                                });
                    }

                } catch (IOException e) {
                    replyEmbed(event, "⚠ Fehler beim Speichern des Transkripts: " + e.getMessage(), Color.RED);
                    return;
                }

                ticketChannel.sendMessageEmbeds(new EmbedBuilder()
                        .setDescription(Lang.get("ticket_closing_broadcast"))
                        .setColor(Color.RED)
                        .build()).queue();

                ticketChannel.delete().queueAfter(5, TimeUnit.SECONDS);

                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                        .setDescription(Lang.get("ticket_closing_success"))
                        .setColor(Color.GREEN)
                        .build()).setEphemeral(true).queue();
            });
            return;
        }

        if (id.startsWith("ticket:send_transcript:")) {
            String channelId = id.split(":")[2];

            // ✅ Hole Log-Channel aus config.yml
            String logChannelId = Synccord.getInstance().getConfig().getString("tickets.log_channel_id");
            TextChannel logChannel = event.getGuild().getTextChannelById(logChannelId);

            File ticketDir = new File("ticket");
            File foundFile = null;

            if (ticketDir.exists()) {
                for (File subDir : ticketDir.listFiles()) {
                    File file = new File(subDir, channelId + ".txt");
                    if (file.exists()) {
                        foundFile = file;
                        break;
                    }
                }
            }

            if (foundFile == null || logChannel == null) {
                replyEmbed(event, Lang.get("ticket_transcript_not_found"), Color.RED);
                return;
            }

            try {
                ByteArrayInputStream input = new ByteArrayInputStream(Files.readAllBytes(foundFile.toPath()));
                FileUpload file = FileUpload.fromData(input, "transcript.txt");

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("📁 Transkript für Ticket")
                        .setDescription("Das Transkript wird in 5 Minuten gelöscht.")
                        .setColor(Color.DARK_GRAY)
                        .setTimestamp(Instant.now());

                File fileToDelete = foundFile;
                logChannel.sendMessageEmbeds(embed.build())
                        .addFiles(file)
                        .queue(msg -> {
                            msg.delete().queueAfter(5, TimeUnit.MINUTES);
                            fileToDelete.delete();
                        });

                replyEmbed(event, Lang.get("ticket_transcript_sent"), Color.GREEN);
            } catch (IOException e) {
                replyEmbed(event, "⚠ Fehler beim Laden des Transkripts: " + e.getMessage(), Color.RED);
            }
            return;
        }
        if (id.startsWith("ticket:cancel_close:")) {
            replyEmbed(event, Lang.get("ticket_close_cancelled"), Color.GRAY);
            return;
        }

        String[] idParts = id.split(":");
        if (idParts.length == 2 && idParts[0].equals("ticket")) {
            String ticketId = idParts[1];
            TicketType type = ticketManager.getTicketTypeById(ticketId);
            if (type == null) {
                replyEmbed(event, Lang.get("ticket_type_not_found"), Color.RED);
                return;
            }

            Modal.Builder modal = Modal.create("ticketmodal:" + type.getId(), type.getName());
            for (Map.Entry<Integer, TicketQuestion> entry : type.getQuestions().entrySet()) {
                TicketQuestion tq = entry.getValue();
                TextInput input = TextInput.create("q" + entry.getKey(), String.join(" ", tq.getQuestions()), TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setMaxLength(tq.getInputLimit())
                        .build();
                modal.addActionRow(input);
            }
            event.replyModal(modal.build()).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String[] idParts = event.getModalId().split(":");
        if (idParts.length != 2 || !idParts[0].equals("ticketmodal")) {
            return;
        }

        String ticketId = idParts[1];
        TicketType type = ticketManager.getTicketTypeById(ticketId);
        if (type == null || event.getMember() == null) {
            return;
        }

        channelCreator.createTicketChannel(event, event.getMember(), type);
    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
        if (!event.getComponentId().equals("ticket:add_user_select")) {
            return;
        }
        if (event.getMentions().getMembers().isEmpty()) {
            replyEmbed(event, Lang.get("ticket_user_select_none"), Color.GRAY);
            return;
        }

        Member target = event.getMentions().getMembers().get(0);
        if (event.getChannel() instanceof TextChannel channel) {
            channel.upsertPermissionOverride(target)
                    .setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                    .queue(
                            success -> replyEmbed(event, Lang.get("ticket_user_added").replace("%user%", target.getAsMention()), Color.GREEN),
                            error -> replyEmbed(event, Lang.get("ticket_user_add_error").replace("%error%", error.getMessage()), Color.RED)
                    );
        } else {
            replyEmbed(event, Lang.get("ticket_channel_no_permissions"), Color.RED);
        }
    }

    private void replyEmbed(ButtonInteractionEvent event, String message, Color color) {
        event.replyEmbeds(new EmbedBuilder().setDescription(message).setColor(color).setTimestamp(Instant.now()).build()).setEphemeral(true).queue();
    }

    private void replyEmbed(EntitySelectInteractionEvent event, String message, Color color) {
        event.replyEmbeds(new EmbedBuilder().setDescription(message).setColor(color).setTimestamp(Instant.now()).build()).setEphemeral(true).queue();
    }

    public TicketType getTypeByChannel(TextChannel channel) {
        String name = channel.getName(); // z. B. "support-1234"
        for (TicketType type : ticketManager.getTicketTypes()) {
            if (name.startsWith(type.getId() + "-")) {
                return type;
            }
        }
        return null;
    }

    public TicketType loadTicketTypeByChannelId(String channelId) {
        File file = new File("tickets", channelId + ".yml");
        if (!file.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String typeId = config.getString("ticket_id");

        if (typeId == null) {
            return null;
        }

        return ticketManager.getTicketTypeById(typeId);
    }

    private void debug(String key, String value) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("[Debug] " + Lang.get(key)
                    .replace("%value%", value)
                    .replace("%ticket%", value)
                    .replace("%channel%", value)
                    .replace("%role%", value));
        }
    }

    private void debug(String key) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("[Debug] " + Lang.get(key));
        }
    }
}
