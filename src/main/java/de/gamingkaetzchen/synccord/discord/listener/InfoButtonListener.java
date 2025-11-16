package de.gamingkaetzchen.synccord.discord.listener;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class InfoButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (buttonId == null || !buttonId.equals("show_players")) {
            return;
        }

        debug("debug_button_click", "%user%", event.getUser().getAsTag());

        Member member = event.getMember();
        if (member == null) {
            event.reply(Lang.get("no_permission")).setEphemeral(true).queue();
            return;
        }

        // 1) Rollen aus der config holen
        List<String> allowedRoles = Synccord.getInstance()
                .getConfig()
                .getStringList("info.allowed-roles");

        boolean hasPermission = false;

        if (allowedRoles != null && !allowedRoles.isEmpty()) {
            // 2) prüfen ob Member eine dieser Rollen hat
            hasPermission = member.getRoles().stream()
                    .anyMatch(role -> allowedRoles.contains(role.getId()));
        } else {
            // 3) Fallback: wenn nix in config steht -> Admin-Recht wie bisher
            hasPermission = member.hasPermission(Permission.ADMINISTRATOR);
        }

        if (!hasPermission) {
            debug("debug_button_no_permission", "%user%", event.getUser().getAsTag());
            event.reply(Lang.get("no_permission")).setEphemeral(true).queue();
            return;
        }

        String players = Bukkit.getOnlinePlayers().stream()
                .map(p -> "• " + p.getName())
                .collect(Collectors.joining("\n"));

        if (players.isEmpty()) {
            players = Lang.get("info_players_empty");
        }

        debug("debug_sending_player_list");

        event.reply("**" + Lang.get("info_players_title") + "**\n" + players)
                .setEphemeral(true)
                .queue();
    }

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }

    private void debug(String key) {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(Lang.get(key));
        }
    }

    private void debug(String key, String placeholder, String value) {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get(key).replace(placeholder, value)
            );
        }
    }
}
