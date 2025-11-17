package de.gamingkaetzchen.synccord.discord.commands;

import java.util.UUID;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.database.DatabaseManager;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class UnlinkMCCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase("unlinkmc")) {
            return;
        }

        // Admin-Prüfung
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(
                        Lang.get("debug_dcfind_no_permission")
                                .replace("%sender%", event.getUser().getName())
                );
            }

            event.reply(Lang.get("no_permission")).setEphemeral(true).queue();
            return;
        }

        var uuidOption = event.getOption("uuid");
        if (uuidOption == null) {

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(
                        Lang.get("debug_dcfind_wrong_usage")
                                .replace("%sender%", event.getUser().getName())
                );
            }

            event.reply(Lang.get("unlinkmc_missing_uuid")).setEphemeral(true).queue();
            return;
        }

        String uuidStr = uuidOption.getAsString();
        try {
            UUID uuid = UUID.fromString(uuidStr);

            if (!DatabaseManager.isLinked(uuid)) {
                if (isDebug()) {
                    Synccord.getInstance().getLogger().info(
                            Lang.get("debug_unlinkmc_not_linked").replace("%uuid%", uuidStr));
                }

                event.reply(Lang.get("unlinkmc_not_linked").replace("%uuid%", uuidStr))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            DatabaseManager.unlink(uuid);

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(
                        Lang.get("debug_unlinkmc_success").replace("%uuid%", uuid.toString()));
            }

            event.reply(Lang.get("unlinkmc_success").replace("%uuid%", uuid.toString()))
                    .setEphemeral(true)
                    .queue();

        } catch (IllegalArgumentException e) {
            if (isDebug()) {
                Synccord.getInstance().getLogger().warning(
                        Lang.get("debug_unlinkmc_invalid_uuid").replace("%uuid%", uuidStr));
            }
            event.reply(Lang.get("unlinkmc_invalid_uuid")).setEphemeral(true).queue();
        }
    }

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }
}
