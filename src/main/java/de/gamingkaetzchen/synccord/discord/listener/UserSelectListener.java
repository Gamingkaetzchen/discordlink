package de.gamingkaetzchen.synccord.discord.listener;

import org.jetbrains.annotations.NotNull;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class UserSelectListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!"ticket:add_user_select".equals(event.getComponentId())) {
            return;
        }

        if (event.getGuild() == null) {
            // Fallback, sollte normal nie passieren
            event.reply(Lang.get("ticket_channel_no_permissions"))
                    .setEphemeral(true)
                    .queue();
            debug("debug_userselect_not_found", "null-guild");
            return;
        }

        // ausgewählten User aus dem Menü holen
        if (event.getValues().isEmpty()) {
            event.reply(Lang.get("ticket_user_not_found"))
                    .setEphemeral(true)
                    .queue();
            debug("debug_userselect_not_found", "no-value");
            return;
        }

        String userId = event.getValues().get(0);
        Member memberToAdd = event.getGuild().getMemberById(userId);

        if (memberToAdd == null) {
            event.reply(Lang.get("ticket_user_not_found"))
                    .setEphemeral(true)
                    .queue();
            debug("debug_userselect_not_found", userId);
            return;
        }

        // Channel als TextChannel erzwingen (sollte es hier immer sein)
        if (event.getChannel().asGuildMessageChannel().getType().isThread()) {
            // Threads o.ä. nicht bearbeiten
            event.reply(Lang.get("ticket_channel_no_permissions"))
                    .setEphemeral(true)
                    .queue();
            debug("debug_userselect_not_found", "not-textchannel");
            return;
        }

        // Benutzer für den Ticket-Channel berechtigen
        event.getChannel().asTextChannel()
                .upsertPermissionOverride(memberToAdd)
                .setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                .queue();

        event.reply(
                Lang.get("ticket_user_added")
                        .replace("%user%", memberToAdd.getAsMention())
        )
                .setEphemeral(true)
                .queue();

        debug("debug_userselect_success", userId);
    }

    private void debug(String key, String value) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info(
                    "[Debug] " + Lang.get(key).replace("%user%", value)
            );
        }
    }
}
