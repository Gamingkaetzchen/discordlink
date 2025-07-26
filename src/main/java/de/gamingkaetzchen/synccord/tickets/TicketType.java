package de.gamingkaetzchen.synccord.tickets;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TicketType {

    private final String id;
    private final String name;
    private final String description;
    private final String buttonName;
    private final String categoryId;
    private final List<String> supporterRoles;
    private final Map<Integer, TicketQuestion> questions;
    private final Map<String, TicketType> ticketTypes = new HashMap<>();

    public TicketType(String id, String name, String description, String buttonName, String categoryId, List<String> supporterRoles, Map<Integer, TicketQuestion> questions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.buttonName = buttonName;
        this.categoryId = categoryId;
        this.supporterRoles = supporterRoles;
        this.questions = questions;
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

    public EmbedBuilder toFancyEmbed() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📩 " + getName());
        embed.setDescription(getDescription());
        embed.setColor(0x2F3136);

        if (Synccord.getInstance().getDiscordBot() != null && Synccord.getInstance().getDiscordBot().getJDA() != null) {
            String botAvatar = Synccord.getInstance().getDiscordBot().getJDA().getSelfUser().getEffectiveAvatarUrl();
            embed.setThumbnail(botAvatar);
            embed.setFooter(Lang.get("ticket_embed_footer"), botAvatar);
        } else {
            embed.setFooter(Lang.get("ticket_embed_footer"));
        }

        if (categoryId != null && !categoryId.isEmpty()) {
            embed.addField("📁 Kategorie", "<#" + categoryId + ">", true);
        }

        if (supporterRoles != null && !supporterRoles.isEmpty()) {
            String rolesFormatted = supporterRoles.stream()
                    .map(roleId -> "<@&" + roleId + ">")
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("N/A");
            embed.addField("👥 Supporter-Rollen", rolesFormatted, true);
        }

        embed.addField("❓ Fragen", String.valueOf(questions.size()), true);
        return embed;
    }

    public Collection<TicketType> getTicketTypes() {
        return ticketTypes.values();
    }

}
