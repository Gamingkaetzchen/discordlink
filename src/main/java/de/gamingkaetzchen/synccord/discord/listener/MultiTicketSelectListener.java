package de.gamingkaetzchen.synccord.discord.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.tickets.TicketManager;
import de.gamingkaetzchen.synccord.tickets.TicketType;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class MultiTicketSelectListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith("multiticket-select:")) {
            return;
        }

        // multiticket-select:<userId>:<channelId>
        String[] parts = componentId.split(":");
        if (parts.length < 3) {
            event.reply(Lang.get("multiticket_invalid_id"))
                    .setEphemeral(true).queue();
            return;
        }

        long ownerId = Long.parseLong(parts[1]);
        long targetChannelId = Long.parseLong(parts[2]);

        if (event.getUser().getIdLong() != ownerId) {
            // not_your_setup: "❌ Dieses Setup gehört nicht dir, %user%."
            event.reply(Lang.get("not_your_setup").replace("%user%", event.getUser().getName()))
                    .setEphemeral(true).queue();
            return;
        }

        Synccord plugin = Synccord.getInstance();
        TicketManager tm = plugin.getDiscordBot().getTicketManager();

        List<String> selected = event.getValues();
        if (selected.isEmpty()) {
            event.reply(Lang.get("multiticket_no_type_selected"))
                    .setEphemeral(true).queue();
            return;
        }

        TextChannel target = event.getJDA().getTextChannelById(targetChannelId);
        if (target == null) {
            event.reply(Lang.get("multiticket_target_not_found"))
                    .setEphemeral(true).queue();
            return;
        }

        String guildIconUrl = event.getGuild() != null ? event.getGuild().getIconUrl() : null;

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(Lang.get("multiticket_embed_title"))
                .setDescription(Lang.get("multiticket_embed_description"))
                .setColor(0x6E0B0B)
                .setThumbnail(guildIconUrl)
                .setFooter(Lang.get("ticket_embed_footer"), guildIconUrl);

        List<Button> buttons = new ArrayList<>();

        for (String ticketId : selected) {
            TicketType type = tm.getTicketTypeById(ticketId);
            if (type == null) {
                continue;
            }

            String kategorie = (type.getCategoryId() != null && !type.getCategoryId().isEmpty())
                    ? "<#" + type.getCategoryId() + ">"
                    : Lang.get("multiticket_category_none");

            String rollen;
            if (type.getSupporterRoles() != null && !type.getSupporterRoles().isEmpty()) {
                rollen = type.getSupporterRoles().stream()
                        .map(r -> "<@&" + r + ">")
                        .collect(Collectors.joining("\n"));
            } else {
                rollen = Lang.get("multiticket_roles_none");
            }

            int fragen = type.getQuestions() != null ? type.getQuestions().size() : 0;

            // Feld 1: Ticketname + Kategorie
            String ticketTitle = "🎫 " + type.getName();
            String categoryValue = Lang.get("ticket_panel_category_value")
                    .replace("{category}", kategorie);
            eb.addField(ticketTitle, categoryValue, true);

            // Feld 2: Rollen
            eb.addField(
                    Lang.get("ticket_panel_roles_label"),
                    Lang.get("ticket_panel_roles_value").replace("{roles}", rollen),
                    true
            );

            // Feld 3: Fragen
            eb.addField(
                    Lang.get("ticket_panel_questions_label"),
                    Lang.get("ticket_panel_questions_value")
                            .replace("{count}", String.valueOf(fragen)),
                    true
            );

            // Buttons → Ticketname als Beschriftung
            buttons.add(Button.primary("ticket:" + type.getId(), type.getName()));
        }

        target.sendMessageEmbeds(eb.build())
                .setActionRow(buttons)
                .queue(msg -> {
                    event.reply(
                            Lang.get("multiticket_created_success")
                                    .replace("%channel%", target.getAsMention())
                    )
                            .setEphemeral(true).queue();
                });

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(
                    Lang.get("debug_multiticket_embed_created")
                            .replace("%user%", event.getUser().getName())
                            .replace("%channel%", target.getName())
            );
        }
    }
}
