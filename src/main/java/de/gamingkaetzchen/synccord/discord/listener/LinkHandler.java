package de.gamingkaetzchen.synccord.discord.listener;

import java.util.UUID;

import org.bukkit.Bukkit;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.database.DatabaseManager;
import de.gamingkaetzchen.synccord.discord.LinkManager;
import de.gamingkaetzchen.synccord.discord.util.RoleSyncUtil;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class LinkHandler extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("link_code")) {
            return;
        }

        TextInput input = TextInput.create("link_code_input", Lang.get("link_modal_input_label"), TextInputStyle.SHORT)
                .setPlaceholder(Lang.get("link_modal_input_placeholder"))
                .setMinLength(6)
                .setMaxLength(6)
                .build();

        Modal modal = Modal.create("link_modal", Lang.get("link_modal_title"))
                .addActionRow(input)
                .build();

        event.replyModal(modal).queue();
        debugLog(Lang.get("debug_link_modal_opened").replace("%user%", event.getUser().getAsTag()));
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("link_modal")) {
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            event.reply(Lang.get("link_error_generic")).setEphemeral(true).queue();
            return;
        }

        if (DatabaseManager.isDiscordLinked(member.getId())) {
            event.reply(Lang.get("link_error_already_linked")).setEphemeral(true).queue();
            debugLog(Lang.get("debug_link_already_linked").replace("%id%", member.getId()));
            return;
        }

        String inputCode = event.getValue("link_code_input").getAsString().toUpperCase();
        debugLog(Lang.get("debug_link_code_received").replace("%id%", member.getId()).replace("%code%", inputCode));

        UUID uuid = LinkManager.getUUIDByCode(inputCode);

        if (uuid == null) {
            event.reply(Lang.get("link_error_invalid_code")).setEphemeral(true).queue();
            debugLog(Lang.get("debug_link_invalid_code").replace("%code%", inputCode));
            return;
        }

        LinkManager.link(uuid, member.getId());

        Bukkit.getScheduler().runTask(Synccord.getInstance(), () -> {
            Synccord.getInstance().getLogger().info("🔗 Spieler " + uuid + " wurde mit Discord-Nutzer " + member.getId() + " verknüpft.");
            RoleSyncUtil.syncRolesToMinecraft(member, uuid); // ✅ nur Discord → Minecraft
        });

        event.reply(Lang.get("link_success")).setEphemeral(true).queue();
        debugLog(Lang.get("debug_link_success").replace("%uuid%", uuid.toString()).replace("%id%", member.getId()));
    }

    private void debugLog(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
