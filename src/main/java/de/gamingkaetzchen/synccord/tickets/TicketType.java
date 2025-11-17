package de.gamingkaetzchen.synccord.tickets;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;

public class TicketType {

    private final String id;
    private final String name;
    private final String description;
    private final String buttonName;
    private final String categoryId;
    private final List<String> supporterRoles;
    private final Map<Integer, TicketQuestion> questions;

    // pro Tickettyp konfigurierbar
    private final boolean litebansHook;

    public TicketType(
            String id,
            String name,
            String description,
            String buttonName,
            String categoryId,
            List<String> supporterRoles,
            Map<Integer, TicketQuestion> questions,
            boolean litebansHook
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.buttonName = buttonName;
        this.categoryId = categoryId;
        this.supporterRoles = supporterRoles;
        this.questions = questions;
        this.litebansHook = litebansHook;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getButtonName() {
        return buttonName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public List<String> getSupporterRoles() {
        return supporterRoles;
    }

    public Map<Integer, TicketQuestion> getQuestions() {
        return questions;
    }

    public boolean isLitebansHook() {
        return litebansHook;
    }

    public EmbedBuilder toFancyEmbed() {
        EmbedBuilder embed = new EmbedBuilder();

        // Titel & globale Panel-Beschreibung aus Langfile
        embed.setTitle(Lang.get("ticket_panel_title"));
        embed.setDescription(Lang.get("ticket_panel_description"));
        embed.setColor(0x2F3136);

        // Bot-Avatar + Footer aus Langfile
        String botAvatar = null;
        if (Synccord.getInstance().getDiscordBot() != null
                && Synccord.getInstance().getDiscordBot().getJDA() != null) {
            botAvatar = Synccord.getInstance().getDiscordBot().getJDA()
                    .getSelfUser()
                    .getEffectiveAvatarUrl();
            embed.setThumbnail(botAvatar);
            embed.setFooter(Lang.get("ticket_embed_footer"), botAvatar);
        } else {
            embed.setFooter(Lang.get("ticket_embed_footer"));
        }

        // ✅ Ticket-spezifische Beschreibung aus config.yml (falls gesetzt)
        if (description != null && !description.isBlank()) {
            embed.addField(
                    Lang.get("ticket_panel_desc_label"), // neuer Lang-Key
                    description,
                    false
            );
        }

        // Kategorie-Feld
        if (categoryId != null && !categoryId.isEmpty()) {
            String categoryDisplay = "<#" + categoryId + ">";
            embed.addField(
                    Lang.get("ticket_panel_category_label"),
                    Lang.get("ticket_panel_category_value").replace("{category}", categoryDisplay),
                    true
            );
        }

        // Supporter-Rollen-Feld
        if (supporterRoles != null && !supporterRoles.isEmpty()) {
            String rolesFormatted = supporterRoles.stream()
                    .map(roleId -> "<@&" + roleId + ">")
                    .collect(Collectors.joining("\n"));

            embed.addField(
                    Lang.get("ticket_panel_roles_label"),
                    Lang.get("ticket_panel_roles_value").replace("{roles}", rolesFormatted),
                    true
            );
        }

        // Fragen-Feld
        int questionCount = (questions != null) ? questions.size() : 0;
        String questionCountStr = String.valueOf(questionCount);

        embed.addField(
                Lang.get("ticket_panel_questions_label"),
                Lang.get("ticket_panel_questions_value").replace("{count}", questionCountStr),
                true
        );

        // LiteBans-Hinweis kommt wie gehabt im TicketChannelCreator dazu
        return embed;
    }
}
