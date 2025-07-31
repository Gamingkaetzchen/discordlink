package de.gamingkaetzchen.synccord.discord.commands;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class EmbitCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("embit")) {
            return;
        }

        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply(Lang.get("no_permission")).setEphemeral(true).queue();
            return;
        }

        // Debug-Ausgabe
        if (Synccord.getInstance().isDebug()) {
            Synccord.getInstance().getLogger().info(Lang.get("debug_embit_opened"));
        }

        // Modal-Felder mit Langfile-Support
        TextInput title = TextInput.create("title", Lang.get("embit_modal_input_title"), TextInputStyle.SHORT)
                .setRequired(true).setMaxLength(100).build();

        TextInput content = TextInput.create("content", Lang.get("embit_modal_input_content"), TextInputStyle.PARAGRAPH)
                .setRequired(true).setMaxLength(4000).build();

        TextInput image = TextInput.create("image", Lang.get("embit_modal_input_image"), TextInputStyle.SHORT)
                .setRequired(false).setMaxLength(400).build();

        TextInput footer = TextInput.create("footer", Lang.get("embit_modal_input_footer"), TextInputStyle.SHORT)
                .setRequired(false).setMaxLength(200).build();

        TextInput color = TextInput.create("color", Lang.get("embit_modal_input_color"), TextInputStyle.SHORT)
                .setRequired(false).setMaxLength(7).build();

        Modal modal = Modal.create("embit_modal", Lang.get("embit_modal_title"))
                .addActionRow(title)
                .addActionRow(content)
                .addActionRow(image)
                .addActionRow(footer)
                .addActionRow(color)
                .build();

        event.replyModal(modal).queue();
    }

    public static CommandData getCommandData() {
        return Commands.slash("embit", Lang.get("embit_command_description"));
    }
}
