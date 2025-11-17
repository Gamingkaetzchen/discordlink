package de.gamingkaetzchen.synccord.discord.listener;

import java.awt.Color;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EmbitListener extends ListenerAdapter {

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("embit_modal")) {
            return;
        }

        // Eingaben sammeln
        String title = event.getValue("title") != null ? event.getValue("title").getAsString().trim() : "";
        String content = event.getValue("content") != null ? event.getValue("content").getAsString().trim() : "";
        String image = event.getValue("image") != null ? event.getValue("image").getAsString().trim() : "";
        String footer = event.getValue("footer") != null ? event.getValue("footer").getAsString().trim() : "";
        String colorInput = event.getValue("color") != null ? event.getValue("color").getAsString().trim() : "";

        String colorHex = colorInput.isEmpty() ? "#2ECC71" : colorInput;
        Color color;

        try {
            color = Color.decode(colorHex);
        } catch (NumberFormatException e) {
            color = Color.decode("#2ECC71"); // Fallback-Farbe

            if (isDebug()) {
                Synccord.getInstance().getLogger().warning(
                        Lang.get("debug_embit_invalid_color")
                                .replace("%color%", colorHex)
                );
            }
        }

        // Debug-Ausgabe
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get("debug_embit_submitted")
                            .replace("%title%", title)
                            .replace("%content%", content)
                            .replace("%image%", image)
                            .replace("%footer%", footer)
                            .replace("%color%", colorHex)
            );
        }

        try {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(content)
                    .setColor(color);

            if (!image.isEmpty()) {
                embed.setImage(image);
            }
            if (!footer.isEmpty()) {
                embed.setFooter(footer);
            }

            event.replyEmbeds(embed.build()).queue();

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(Lang.get("embit_success"));
            }

        } catch (Exception e) {
            event.reply(Lang.get("embit_fail")).setEphemeral(true).queue();
            if (isDebug()) {
                e.printStackTrace();
            }
        }
    }

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }
}
