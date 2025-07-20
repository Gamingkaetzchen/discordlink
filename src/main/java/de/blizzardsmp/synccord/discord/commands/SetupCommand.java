package de.blizzardsmp.synccord.discord.commands;

import java.awt.Color;
import java.util.List;

import de.blizzardsmp.synccord.Synccord;
import de.blizzardsmp.synccord.discord.InfoUpdater;
import de.blizzardsmp.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

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
            Synccord.getInstance().getLogger().info(Lang.get("debug_setup_used").replace("%type%", type));
        }

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

        } else if (type.equalsIgnoreCase("info")) {
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

            event.reply("✅ " + Lang.get("info_embed_sent")).setEphemeral(true).queue();

        } else {
            event.reply(Lang.get("invalid_type")).setEphemeral(true).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals("setup") || !event.getFocusedOption().getName().equals("type")) {
            return;
        }

        List<Command.Choice> choices = List.of(
                new Command.Choice("linking", "linking"),
                new Command.Choice("info", "info")
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
