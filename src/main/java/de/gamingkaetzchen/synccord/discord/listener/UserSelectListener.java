package de.gamingkaetzchen.synccord.discord.listener;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class UserSelectListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("ticket:add_user_select")) {
            return;
        }

        String userId = event.getValues().get(0);
        Member memberToAdd = event.getGuild().getMemberById(userId);

        if (memberToAdd == null) {
            event.reply(Lang.get("ticket_user_not_found")).setEphemeral(true).queue();
            debug("debug_userselect_not_found", userId);
            return;
        }

        event.getChannel().asTextChannel().getManager().putPermissionOverride(
                memberToAdd,
                Permission.VIEW_CHANNEL.getRawValue() | Permission.MESSAGE_SEND.getRawValue(),
                0
        ).queue();

        event.reply(Lang.get("ticket_user_added").replace("%user%", memberToAdd.getAsMention()))
                .setEphemeral(true).queue();

        debug("debug_userselect_success", userId);
    }

    private void debug(String key, String value) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("[Debug] " + Lang.get(key).replace("%user%", value));
        }
    }
}
