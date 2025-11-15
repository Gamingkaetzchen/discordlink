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
            event.reply("❌ Ungültige MultiTicket-ID. Bitte `/setup multiticket` neu ausführen.")
                    .setEphemeral(true).queue();
            return;
        }

        long ownerId = Long.parseLong(parts[1]);
        long targetChannelId = Long.parseLong(parts[2]);

        if (event.getUser().getIdLong() != ownerId) {
            event.reply(Lang.get("not_your_setup", java.util.Map.of("user", event.getUser().getName())))
                    .setEphemeral(true).queue();
            return;
        }

        Synccord plugin = Synccord.getInstance();
        TicketManager tm = plugin.getDiscordBot().getTicketManager();

        List<String> selected = event.getValues();
        if (selected.isEmpty()) {
            event.reply("⚠ Bitte mindestens einen Ticket-Typ auswählen.").setEphemeral(true).queue();
            return;
        }

        TextChannel target = event.getJDA().getTextChannelById(targetChannelId);
        if (target == null) {
            event.reply("❌ Zielkanal nicht gefunden.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("📨 Support Ticket")
                .setDescription("Wähle unten die passende Kategorie aus.")
                .setColor(0x6E0B0B)
                .setThumbnail(event.getGuild() != null ? event.getGuild().getIconUrl() : null)
                .setFooter("Ticket-System | Synccord");

        List<Button> buttons = new ArrayList<>();

        for (String ticketId : selected) {
            TicketType type = tm.getTicketTypeById(ticketId);
            if (type == null) {
                continue;
            }

            String kategorie = (type.getCategoryId() != null && !type.getCategoryId().isEmpty())
                    ? "<#" + type.getCategoryId() + ">"
                    : "_keine_";

            String rollen;
            if (type.getSupporterRoles() != null && !type.getSupporterRoles().isEmpty()) {
                rollen = type.getSupporterRoles().stream()
                        .map(r -> "<@&" + r + ">")
                        .collect(Collectors.joining("\n"));
            } else {
                rollen = "_keine_";
            }

            int fragen = type.getQuestions() != null ? type.getQuestions().size() : 0;

            // ⬅️ 1. Feld: Ticketname + Kategorie
            eb.addField("🎫 " + type.getName(), "📁 " + kategorie, true);
            // 2. Feld: Rollen
            eb.addField("🧑‍💻 Supporter-Rollen", rollen, true);
            // 3. Feld: Fragen
            eb.addField("❓ Fragen", String.valueOf(fragen), true);

            // Buttons → immer den Ticketnamen anzeigen (Bug 3)
            buttons.add(Button.primary("ticket:" + type.getId(), type.getName()));
        }

        target.sendMessageEmbeds(eb.build())
                .setActionRow(buttons)
                .queue(msg -> {
                    event.reply("✅ Multi-Ticket-Embed wurde in " + target.getAsMention() + " erstellt.")
                            .setEphemeral(true).queue();
                });

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Debug] Multi-Ticket-Embed von " + event.getUser().getName()
                    + " in #" + target.getName() + " erstellt.");
        }
    }
}
