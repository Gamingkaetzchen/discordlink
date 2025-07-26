package de.gamingkaetzchen.synccord.discord.listener;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.LinkManager;
import de.gamingkaetzchen.synccord.tickets.TicketManager;
import de.gamingkaetzchen.synccord.tickets.TicketQuestion;
import de.gamingkaetzchen.synccord.tickets.TicketType;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TicketChannelCreator {

    private final TicketManager ticketManager;

    public TicketChannelCreator(TicketManager ticketManager) {
        this.ticketManager = ticketManager;
    }

    public void createTicketChannel(ModalInteractionEvent event, Member member, TicketType type) {
        String categoryId = type.getCategoryId();
        if (categoryId == null || categoryId.isBlank()) {
            event.reply(Lang.get("ticket_create_no_category")).setEphemeral(true).queue();
            debug("debug_ticket_no_category", type.getId());
            return;
        }

        Category category = event.getJDA().getCategoryById(categoryId);
        if (category == null) {
            event.reply(Lang.get("ticket_create_category_not_found")).setEphemeral(true).queue();
            debug("debug_ticket_category_not_found", categoryId);
            return;
        }

        String ticketChannelName = "ticket-" + member.getEffectiveName().toLowerCase();

        category.createTextChannel(ticketChannelName)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, List.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(member, List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), List.of())
                .queue(channel -> {

                    debug("debug_ticket_channel_created", ticketChannelName);

                    for (String roleId : type.getSupporterRoles()) {
                        Role role = event.getGuild().getRoleById(roleId);
                        if (role != null) {
                            channel.upsertPermissionOverride(role)
                                    .setAllowed(List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                                    .queue();
                            debug("debug_ticket_support_role_added", roleId);
                        }
                    }

                    EmbedBuilder embed = type.toFancyEmbed();

                    String discordId = member.getId();
                    Optional<UUID> uuidOpt = LinkManager.getUUID(discordId);
                    if (uuidOpt.isPresent()) {
                        UUID uuid = uuidOpt.get();
                        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
                        embed.addField("🧍 " + Lang.get("ticket_field_name"),
                                offline.getName() != null ? offline.getName() : "Unbekannt", true);

                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            Location loc = player.getLocation();
                            World world = loc.getWorld();
                            if (world != null) {
                                embed.addField("🌍 " + Lang.get("ticket_field_world"), world.getName(), true);
                            }
                            embed.addField("📍 " + Lang.get("ticket_field_coordinates"),
                                    String.format("x: %.0f, y: %.0f, z: %.0f", loc.getX(), loc.getY(), loc.getZ()), false);
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<Integer, TicketQuestion> entry : type.getQuestions().entrySet()) {
                        var value = event.getValue("q" + entry.getKey());
                        if (value == null) {
                            continue;
                        }

                        String input = value.getAsString();
                        String question = String.join(" ", entry.getValue().getQuestions());
                        sb.append("**").append(entry.getKey()).append("️⃣ ").append(question).append("**\n> ")
                                .append(input).append("\n\n");
                    }

                    embed.addField("📝 " + Lang.get("ticket_field_answers"), sb.toString().trim(), false);

                    channel.sendMessageEmbeds(embed.build())
                            .addActionRow(
                                    Button.primary("ticket:add_user", Lang.get("ticket_button_add_user")),
                                    Button.success("ticket:claim", Lang.get("ticket_button_claim")),
                                    Button.danger("ticket:close", Lang.get("ticket_button_close"))
                            ).queue();

                    // 🔐 Tickettyp in Datei speichern
                    File dir = new File("tickets");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    File file = new File(dir, channel.getId() + ".yml");
                    YamlConfiguration config = new YamlConfiguration();
                    config.set("ticket_id", type.getId());
                    config.set("user_id", member.getId());

                    if (uuidOpt.isPresent()) {
                        UUID uuid = uuidOpt.get();
                        config.set("minecraft_uuid", uuid.toString());
                        config.set("minecraft_name", Bukkit.getOfflinePlayer(uuid).getName());
                    }

                    try {
                        config.save(file);
                    } catch (IOException e) {
                        Synccord.getInstance().getLogger().warning("[Ticket] Fehler beim Speichern von " + file.getName());
                        e.printStackTrace();
                    }

                    event.reply(Lang.get("ticket_created_user_message")).setEphemeral(true).queue();
                });
    }

    private void debug(String key) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("[Debug] " + Lang.get(key));
        }
    }

    private void debug(String key, String value) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("[Debug] " + Lang.get(key)
                    .replace("%value%", value)
                    .replace("%ticket%", value)
                    .replace("%channel%", value)
                    .replace("%role%", value));
        }
    }
}
