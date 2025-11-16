package de.gamingkaetzchen.synccord.discord.commands;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.InfoUpdater;
import de.gamingkaetzchen.synccord.discord.PlayerListUpdater;
import de.gamingkaetzchen.synccord.tickets.TicketManager;
import de.gamingkaetzchen.synccord.tickets.TicketType;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

public class SetupCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("setup")) {
            return;
        }

        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply(Lang.get("no_permission")).setEphemeral(true).queue();
            return;
        }

        var option = event.getOption("type");
        if (option == null) {
            event.reply(Lang.get("invalid_type")).setEphemeral(true).queue();
            return;
        }

        String type = option.getAsString();
        String guildIconUrl = event.getGuild() != null ? event.getGuild().getIconUrl() : null;

        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get("debug_setup_used").replace("%type%", type));
        }

        // ====================== /setup linking ======================
        if (type.equalsIgnoreCase("linking")) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(Lang.get("link_embed_title"))
                    .setDescription(Lang.get("link_embed_description"))
                    .setColor(Color.GREEN)
                    .setThumbnail(guildIconUrl)
                    .setFooter(Lang.get("footer_text"), guildIconUrl);

            event.replyEmbeds(embed.build())
                    .addActionRow(Button.primary("link_code", Lang.get("link_button")))
                    .queue();

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(Lang.get("debug_setup_linking_sent"));
            }
            return;
        }

        // ====================== /setup info ======================
        if (type.equalsIgnoreCase("info")) {
            EmbedBuilder embed = InfoUpdater.buildStatusEmbed(guildIconUrl);
            MessageChannel channel = event.getChannel();

            channel.sendMessageEmbeds(embed.build())
                    .addActionRow(Button.primary("show_players", "🔍 " + Lang.get("show_players_button")))
                    .submit().thenAccept(msg -> {
                        InfoUpdater.startAutoUpdate(channel, msg);

                        if (isDebug()) {
                            Synccord.getInstance().getLogger().info(Lang.get("debug_setup_info_started"));
                        }
                    });

            event.reply(Lang.get("info_sent")).setEphemeral(true).queue();
            return;
        }

        // ====================== /setup playerlist ======================
        if (type.equalsIgnoreCase("playerlist")) {
            // hübsches Embed mit aktuellem Online-Stand bauen
            EmbedBuilder embed = PlayerListUpdater.buildPlayerListEmbed(guildIconUrl);
            MessageChannel channel = event.getChannel();

            channel.sendMessageEmbeds(embed.build())
                    .submit()
                    .thenAccept(msg -> {
                        // Auto-Update starten (Timer) + Join/Quit Listener triggert zusätzlich refreshNow()
                        PlayerListUpdater.startAutoUpdate(channel, msg);
                    });

            event.reply(Lang.get("setup_playerlist_sent")).setEphemeral(true).queue();
            return;
        }

        // ====================== /setup regel ======================
        if (type.equalsIgnoreCase("regel")) {
            File ruleFile = new File(Synccord.getInstance().getDataFolder(), "rules.yml");
            YamlConfiguration rulesConfig = YamlConfiguration.loadConfiguration(ruleFile);
            List<MessageEmbed> embeds = new ArrayList<>();

            if (!rulesConfig.contains("regeln") || rulesConfig.getConfigurationSection("regeln") == null) {
                event.reply(Lang.get("setup_rule_invalid_or_empty")).setEphemeral(true).queue();
                return;
            }

            for (String section : rulesConfig.getConfigurationSection("regeln").getKeys(false)) {
                List<String> regeln = rulesConfig.getStringList("regeln." + section);
                StringBuilder sb = new StringBuilder();
                for (String regel : regeln) {
                    sb.append("• ").append(regel).append("\n\n");
                }

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("📜 " + section.replace("_", " "))
                        .setDescription(sb.toString().trim())
                        .setColor(Color.ORANGE)
                        .setThumbnail(guildIconUrl)
                        .setFooter(Lang.get("footer_text"), guildIconUrl);
                embeds.add(embed.build());
            }

            event.deferReply().queue((InteractionHook hook) -> {
                MessageChannel channel = event.getChannel();
                if (channel == null) {
                    hook.sendMessage(Lang.get("setup_rule_no_channel")).setEphemeral(true).queue();
                    return;
                }

                if (!embeds.isEmpty()) {
                    for (int i = 0; i < embeds.size() - 1; i++) {
                        channel.sendMessageEmbeds(embeds.get(i)).queue();
                    }

                    channel.sendMessageEmbeds(embeds.get(embeds.size() - 1))
                            .addActionRow(Button.success("rule_accept_button", Lang.get("setup_rule_button")))
                            .queue();
                }

                hook.sendMessage(Lang.get("setup_rule_sent")).setEphemeral(true).queue();
            });

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(Lang.get("debug_setup_regel_sent"));
            }
            return;
        }

        // ====================== /setup multiticket ======================
        if (type.equalsIgnoreCase("multiticket")) {
            var channelOpt = event.getOption("channel");
            if (channelOpt == null || !(channelOpt.getAsChannel() instanceof TextChannel targetChannel)) {
                event.reply(Lang.get("setup_multiticket_no_channel"))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            if (Synccord.getInstance().getDiscordBot() == null
                    || Synccord.getInstance().getDiscordBot().getTicketManager() == null) {
                event.reply(Lang.get("setup_multiticket_system_unavailable"))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            TicketManager ticketManager = Synccord.getInstance().getDiscordBot().getTicketManager();
            Collection<TicketType> allTypes = ticketManager.getAllTicketTypes();
            if (allTypes == null || allTypes.isEmpty()) {
                event.reply(Lang.get("setup_multiticket_no_types"))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            StringSelectMenu.Builder menu = StringSelectMenu.create(
                    "multiticket-select:" + event.getUser().getId() + ":" + targetChannel.getId())
                    .setPlaceholder(Lang.get("setup_multiticket_placeholder"))
                    .setMinValues(1)
                    .setMaxValues(Math.min(5, allTypes.size()));

            for (TicketType tt : allTypes) {
                menu.addOption(tt.getName(), tt.getId());
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(Lang.get("setup_multiticket_title"))
                    .setDescription(
                            Lang.get("setup_multiticket_description")
                                    .replace("%channel%", targetChannel.getAsMention()))
                    .setColor(Color.CYAN);

            event.replyEmbeds(embed.build())
                    .addActionRow(menu.build())
                    .setEphemeral(true)
                    .queue();

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(
                        Lang.get("debug_setup_multiticket_started")
                                .replace("%user%", event.getUser().getName())
                                .replace("%channel%", targetChannel.getId()));
            }
            return;
        }

        // wenn keiner der bekannten Typen
        event.reply(Lang.get("invalid_type")).setEphemeral(true).queue();
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals("setup") || !event.getFocusedOption().getName().equals("type")) {
            return;
        }

        List<Command.Choice> choices = List.of(
                new Command.Choice("linking", "linking"),
                new Command.Choice("info", "info"),
                new Command.Choice("playerlist", "playerlist"),
                new Command.Choice("regel", "regel"),
                new Command.Choice("multiticket", "multiticket")
        );

        event.replyChoices(choices).queue();

        if (isDebug()) {
            Synccord.getInstance().getLogger().info(Lang.get("debug_setup_autocomplete"));
        }
    }

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }
}
