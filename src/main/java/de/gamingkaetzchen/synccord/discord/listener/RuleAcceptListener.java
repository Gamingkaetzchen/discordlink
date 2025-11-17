package de.gamingkaetzchen.synccord.discord.listener;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class RuleAcceptListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!"rule_accept_button".equals(event.getComponentId())) {
            return;
        }

        debug(Lang.get("debug_rule_accept_clicked"));

        TextInput input = TextInput.create(
                "keyword",
                Lang.get("setup_rule_modal_input"),
                TextInputStyle.SHORT)
                .setRequired(true)
                .setMinLength(3)
                .setMaxLength(20)
                .build();

        Modal modal = Modal.create("rule_accept_modal", Lang.get("setup_rule_modal_title"))
                .addActionRow(input)
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!"rule_accept_modal".equals(event.getModalId())) {
            return;
        }

        String input = event.getValue("keyword").getAsString().trim();
        String expected = Synccord.getInstance().getConfig().getString("rules.keyword");
        String roleId = Synccord.getInstance().getConfig().getString("rules.accept-role-id");

        debug(Lang.get("debug_rule_modal_received").replace("%input%", input));

        Member member = event.getMember();
        if (member == null) {
            // eigene Lang-Message statt Hardcode
            event.reply(Lang.get("setup_rule_no_member"))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (input.equalsIgnoreCase(expected)) {
            if (roleId != null && member.getGuild().getRoleById(roleId) != null) {
                member.getGuild().addRoleToMember(member, member.getGuild().getRoleById(roleId)).queue();
            }

            event.reply(Lang.get("setup_rule_success")).setEphemeral(true).queue();
            debug(Lang.get("debug_rule_match"));

        } else {
            event.reply(Lang.get("setup_rule_fail")).setEphemeral(true).queue();
            debug(Lang.get("debug_rule_mismatch"));
        }
    }

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }

    private void debug(String msg) {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
