package de.gamingkaetzchen.synccord.discord.commands;

import java.util.UUID;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.LinkManager;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class LinkMCCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase("linkmc")) {
            return;
        }

        // Permissioncheck
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(
                        Lang.get("debug_dcfind_no_permission")
                                .replace("%sender%", event.getUser().getName())
                );
            }

            event.reply(Lang.get("linkmc_no_permission")).setEphemeral(true).queue();
            return;
        }

        var uuidOption = event.getOption("uuid");
        var discordOption = event.getOption("discordid");

        if (uuidOption == null || discordOption == null) {

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(
                        Lang.get("debug_dcfind_wrong_usage")
                                .replace("%sender%", event.getUser().getName())
                );
            }

            event.reply(Lang.get("linkmc_missing_args")).setEphemeral(true).queue();
            return;
        }

        String uuidStr = uuidOption.getAsString();
        String discordId = discordOption.getAsString();

        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get("debug_linkmc_received")
                            .replace("%uuid%", uuidStr)
                            .replace("%discord%", discordId)
            );
        }

        try {
            UUID uuid = UUID.fromString(uuidStr);

            // einfache Discord-ID-Validierung (17–20 Ziffern)
            if (!discordId.matches("\\d{17,20}")) {
                event.reply(Lang.get("linkmc_invalid_discordid")).setEphemeral(true).queue();
                return;
            }

            LinkManager.link(uuid, discordId);

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(
                        Lang.get("debug_linkmc_success")
                                .replace("%uuid%", uuidStr)
                                .replace("%discord%", discordId)
                );
            }

            event.reply(
                    Lang.get("linkmc_success")
                            .replace("%uuid%", uuidStr)
                            .replace("%discord%", discordId)
            ).setEphemeral(true).queue();

        } catch (IllegalArgumentException e) {
            event.reply(Lang.get("linkmc_invalid_uuid")).setEphemeral(true).queue();

            if (isDebug()) {
                Synccord.getInstance().getLogger().warning(
                        Lang.get("debug_linkmc_invalid_uuid").replace("%uuid%", uuidStr)
                );
            }
        }
    }

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }
}
