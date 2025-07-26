package de.gamingkaetzchen.synccord.discord.commands;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.tickets.TicketManager;
import de.gamingkaetzchen.synccord.tickets.TicketType;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class TicketSetupCommand extends ListenerAdapter {

    private final TicketManager ticketManager;

    public TicketSetupCommand(TicketManager ticketManager, JDA jda) {
        this.ticketManager = ticketManager;

        OptionData typeOption = new OptionData(OptionType.STRING, "type", "Tickettyp-ID aus config.yml", true)
                .setAutoComplete(true);

        OptionData channelOption = new OptionData(OptionType.CHANNEL, "channel", "Channel, in dem der Button gepostet werden soll", true);

        jda.upsertCommand(
                Commands.slash("ticket", "Ticket-System verwalten")
                        .addSubcommands(new SubcommandData("setup", "Ticket-Buttons posten")
                                .addOptions(typeOption, channelOption)
                        )
        ).queue();

        debugLog(Lang.get("debug_ticket_command_registered"));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ticket") || !"setup".equals(event.getSubcommandName())) {
            return;
        }

        event.deferReply(true).queue();

        String ticketId = event.getOption("type").getAsString();
        MessageChannel channel = event.getOption("channel").getAsChannel().asGuildMessageChannel();

        debugLog(Lang.get("debug_ticket_setup_started").replace("%type%", ticketId));

        TicketType type = ticketManager.getTicketTypeById(ticketId);
        if (type == null) {
            event.getHook().sendMessage(Lang.get("ticket_type_not_found")).setEphemeral(true).queue();
            debugLog(Lang.get("debug_ticket_type_not_found").replace("%type%", ticketId));
            return;
        }

        try {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📨 " + type.getName())
                    .setDescription(type.getDescription())
                    .setColor(getColorForTicketType(type.getId()))
                    .addField("🧾 Kategorie", type.getCategoryId() != null ? "<#" + type.getCategoryId() + ">" : "Keine", true)
                    .addField("👥 Supporter-Rollen", formatRoles(type.getSupporterRoles()), true)
                    .addField("❓ Fragen", String.valueOf(type.getQuestions().size()), true)
                    .setFooter("Ticket-System | Synccord", event.getJDA().getSelfUser().getEffectiveAvatarUrl())
                    .setThumbnail(event.getGuild().getIconUrl())
                    .setTimestamp(Instant.now());

            Button button = Button.primary("ticket:" + type.getId(), type.getButtonName());
            channel.sendMessageEmbeds(embed.build()).addActionRow(button).queue();

            event.getHook().sendMessage(Lang.get("ticket_setup_success")).setEphemeral(true).queue();

            debugLog(Lang.get("debug_ticket_setup_success")
                    .replace("%type%", ticketId)
                    .replace("%channel%", channel.getName()));
        } catch (Exception e) {
            event.getHook().sendMessage("❌ Fehler beim Erstellen: " + e.getMessage()).setEphemeral(true).queue();
            e.printStackTrace();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals("ticket")
                || !event.getSubcommandName().equals("setup")
                || !event.getFocusedOption().getName().equals("type")) {
            return;
        }

        List<Choice> choices = ticketManager.getAllTicketTypes().stream()
                .map(type -> new Choice(type.getId(), type.getId()))
                .collect(Collectors.toList());

        event.replyChoices(choices).queue();

        debugLog(Lang.get("debug_setup_autocomplete"));
    }

    private String formatRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "Keine";
        }
        return roles.stream()
                .map(r -> "<@&" + r + ">")
                .collect(Collectors.joining(", "));
    }

    private Color getColorForTicketType(String id) {
        return switch (id.toLowerCase()) {
            case "support" ->
                Color.decode("#3498db");
            case "report" ->
                Color.decode("#e74c3c");
            case "feedback" ->
                Color.decode("#2ecc71");
            default ->
                Color.GRAY;
        };
    }

    private void debugLog(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
