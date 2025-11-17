package de.gamingkaetzchen.synccord.discord.listener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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

public class TicketChannelCreator {

    private final TicketManager ticketManager;

    public TicketChannelCreator(TicketManager ticketManager) {
        this.ticketManager = ticketManager;
    }

    public void createTicketChannel(ModalInteractionEvent event, Member member, TicketType type) {
        String categoryId = type.getCategoryId();
        if (categoryId == null || categoryId.isBlank()) {
            event.reply(Lang.get("ticket_create_no_category")).setEphemeral(true).queue();
            debug("debug_ticket_no_category", categoryId != null ? categoryId : "null");
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

                    // Support-Rollen berechtigen
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

                    // Label für LiteBans-Feld aus Langfile
                    String litebansLabel = "🔒 " + Lang.get("ticket_field_litebans");

                    if (uuidOpt.isPresent()) {
                        UUID uuid = uuidOpt.get();
                        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);

                        embed.addField(
                                "🧍 " + Lang.get("ticket_field_name"),
                                offline.getName() != null
                                ? offline.getName()
                                : Lang.get("ticket_unknown_name"),
                                true
                        );

                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            Location loc = player.getLocation();
                            World world = loc.getWorld();
                            if (world != null) {
                                embed.addField(
                                        "🌍 " + Lang.get("ticket_field_world"),
                                        world.getName(),
                                        true
                                );
                            }
                            embed.addField(
                                    "📍 " + Lang.get("ticket_field_coordinates"),
                                    String.format("x: %.0f, y: %.0f, z: %.0f",
                                            loc.getX(), loc.getY(), loc.getZ()),
                                    false
                            );
                        }

                        // LiteBans anzeigen, wenn für diesen Ticket-Typ aktiviert
                        if (type.isLitebansHook()) {
                            String lbText = fetchLitebansInfo(uuid);
                            if (lbText != null && !lbText.isEmpty()) {
                                embed.addField(litebansLabel, lbText, false);
                            } else {
                                embed.addField(litebansLabel, Lang.get("ticket_litebans_none"), false);
                            }
                        }

                    } else {
                        if (type.isLitebansHook()) {
                            embed.addField(litebansLabel, Lang.get("ticket_litebans_no_link"), false);
                        }
                    }

                    // Antworten aus dem Modal
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<Integer, TicketQuestion> entry : type.getQuestions().entrySet()) {
                        var value = event.getValue("q" + entry.getKey());
                        if (value == null) {
                            continue;
                        }

                        String input = value.getAsString();
                        String question = String.join(" ", entry.getValue().getQuestions());
                        sb.append("**")
                                .append(entry.getKey()).append("️⃣ ")
                                .append(question)
                                .append("**\n> ")
                                .append(input)
                                .append("\n\n");
                    }

                    embed.addField("📝 " + Lang.get("ticket_field_answers"), sb.toString().trim(), false);

                    channel.sendMessageEmbeds(embed.build())
                            .addActionRow(
                                    Button.primary("ticket:add_user", Lang.get("ticket_button_add_user")),
                                    Button.success("ticket:claim", Lang.get("ticket_button_claim")),
                                    Button.danger("ticket:close", Lang.get("ticket_button_close"))
                            ).queue();

                    // Ticket-Datei
                    File dir = new File("tickets");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    File file = new File(dir, channel.getId() + ".yml");
                    YamlConfiguration config = new YamlConfiguration();
                    config.set("ticket_id", type.getId());
                    config.set("user_id", member.getId());
                    uuidOpt.ifPresent(uuid -> {
                        config.set("minecraft_uuid", uuid.toString());
                        config.set("minecraft_name", Bukkit.getOfflinePlayer(uuid).getName());
                    });
                    try {
                        config.save(file);
                    } catch (IOException e) {
                        Synccord.getInstance().getLogger().warning(
                                Lang.get("ticket_save_error")
                                        .replace("%file%", file.getName())
                        );
                        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
                            e.printStackTrace();
                        }
                    }

                    event.reply(Lang.get("ticket_created_user_message")).setEphemeral(true).queue();

                    Bukkit.getScheduler().runTask(Synccord.getInstance(), () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.hasPermission("synccord.ticket.alert")) {
                                String msg = Lang.get("ticket_alert_message")
                                        .replace("%user%", member.getEffectiveName())
                                        .replace("%ticket%", type.getName());
                                player.sendMessage(msg);
                            }
                        }
                    });
                });
    }

    /**
     * Holt LiteBans-Daten (Ban, Mute, 1 Warn) per Reflection aus der aktuell
     * geladenen LiteBans-Version.
     */
    private String fetchLitebansInfo(UUID uuid) {
        var plugin = Synccord.getInstance();
        var log = plugin.getLogger();
        boolean debug = plugin.getConfig().getBoolean("debug", false);

        if (Bukkit.getPluginManager().getPlugin("LiteBans") == null) {
            if (debug) {
                log.info(Lang.get("debug_litebans_not_found"));
            }
            return null;
        }

        String name = Bukkit.getOfflinePlayer(uuid).getName();
        boolean banned = false;
        boolean muted = false;
        String banDetails = null;
        String muteDetails = null;
        List<String> warnDetails = new ArrayList<>();

        try {
            Class<?> dbClass = Class.forName("litebans.api.Database");
            Object db = dbClass.getMethod("get").invoke(null);

            // BAN
            Object banObj = tryObj(db, dbClass,
                    "getBan",
                    new Class<?>[]{UUID.class, String.class, String.class},
                    new Object[]{uuid, name, null});
            if (banObj != null) {
                banned = true;
                banDetails = extractPunishmentInfo(banObj);
            }

            // MUTE
            Object muteObj = tryObj(db, dbClass,
                    "getMute",
                    new Class<?>[]{UUID.class, String.class, String.class},
                    new Object[]{uuid, name, null});
            if (muteObj != null) {
                muted = true;
                muteDetails = extractPunishmentInfo(muteObj);
            }

            // WARNS – deine Version hat keine getWarnings(...), also einzelnes getWarning(...)
            try {
                Object warnObj = dbClass
                        .getMethod("getWarning", UUID.class, String.class, String.class)
                        .invoke(db, uuid, name, null);
                if (warnObj != null) {
                    warnDetails.add(extractPunishmentInfo(warnObj));
                }
            } catch (NoSuchMethodException e) {
                if (debug) {
                    log.info(Lang.get("debug_litebans_no_warning_method"));
                }
            }

        } catch (Throwable t) {
            if (debug) {
                log.warning(
                        Lang.get("debug_litebans_error")
                                .replace("%error%", String.valueOf(t.getMessage()))
                );
                t.printStackTrace();
            }
            return null;
        }

        // Text bauen – komplett über Langfile
        StringBuilder sb = new StringBuilder();

        String yes = Lang.get("litebans_value_yes");   // z.B. **JA**
        String no = Lang.get("litebans_value_no");     // z.B. nein

        String banValue = banned ? yes : no;
        sb.append(Lang.get("litebans_ban_line").replace("{value}", banValue));
        if (banDetails != null) {
            sb.append("\n").append(banDetails);
        }
        sb.append("\n");

        String muteValue = muted ? yes : no;
        sb.append(Lang.get("litebans_mute_line").replace("{value}", muteValue));
        if (muteDetails != null) {
            sb.append("\n").append(muteDetails);
        }
        sb.append("\n");

        if (!warnDetails.isEmpty()) {
            sb.append(
                    Lang.get("litebans_warns_header")
                            .replace("{count}", String.valueOf(warnDetails.size()))
            ).append("\n");

            int i = 1;
            for (String w : warnDetails) {
                sb.append(
                        Lang.get("litebans_warn_entry")
                                .replace("{number}", String.valueOf(i++))
                                .replace("{text}", w)
                ).append("\n");
            }
        } else {
            sb.append(Lang.get("litebans_warns_none"));
        }

        return sb.toString().trim();
    }

    // ========== Helper ==========
    private Object tryObj(Object db, Class<?> dbClass, String name, Class<?>[] params, Object[] args) {
        try {
            var m = dbClass.getMethod(name, params);
            return m.invoke(db, args);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Liest Reason + Ablauf aus einem LiteBans-Punishment.
     */
    private String extractPunishmentInfo(Object pun) {
        boolean debug = Synccord.getInstance().getConfig().getBoolean("debug", false);
        var log = Synccord.getInstance().getLogger();

        Boolean isPermanent = tryBoolGetter(pun, "isPermanent");
        Long expires = null;
        if (isPermanent != null && !isPermanent) {
            expires = readExpiry(pun);
        }

        String reason = null;
        try {
            var m = pun.getClass().getMethod("getReason");
            Object r = m.invoke(pun);
            if (r != null) {
                reason = r.toString();
            }
        } catch (Throwable ignored) {
        }

        StringBuilder sb = new StringBuilder();
        if (reason != null && !reason.isEmpty()) {
            sb.append(
                    Lang.get("litebans_reason_line")
                            .replace("{reason}", reason)
            ).append("\n");
        }

        if (isPermanent != null && isPermanent) {
            sb.append(Lang.get("litebans_duration_perm")).append("\n");
        } else if (expires != null) {
            long now = System.currentTimeMillis();
            if (expires > now) {
                String duration = formatDuration(expires - now);
                sb.append(
                        Lang.get("litebans_expires_in")
                                .replace("{time}", duration)
                ).append("\n");
            } else {
                sb.append(Lang.get("litebans_expired")).append("\n");
            }
        } else {
            String remainingStr = tryStringGetter(pun, "getRemainingDurationString");
            if (remainingStr == null) {
                remainingStr = tryStringGetter(pun, "getDurationString");
            }
            if (remainingStr != null) {
                sb.append(
                        Lang.get("litebans_expires_in")
                                .replace("{time}", remainingStr)
                ).append("\n");
            } else {
                if (debug) {
                    log.info(Lang.get("debug_litebans_no_time"));
                }
                sb.append(Lang.get("litebans_duration_perm")).append("\n");
            }
        }

        return sb.toString().trim();
    }

    private Long readExpiry(Object pun) {
        Long dateEnd = tryNumberGetter(pun, "getDateEnd");
        if (dateEnd != null && dateEnd > 0) {
            if (dateEnd < 10_000_000_000L) {
                dateEnd = dateEnd * 1000L;
            }
            return dateEnd;
        }

        Long remaining = tryNumberGetter(pun, "getRemainingDuration");
        if (remaining != null && remaining > 0) {
            long now = System.currentTimeMillis();
            return now + remaining;
        }

        Long duration = tryNumberGetter(pun, "getDuration");
        Long start = tryNumberGetter(pun, "getDateStart");
        if (duration != null) {
            if (start == null) {
                start = System.currentTimeMillis();
            }
            return start + duration;
        }

        return null;
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return Lang.get("litebans_duration_days_hours")
                    .replace("{days}", String.valueOf(days))
                    .replace("{hours}", String.valueOf(hours % 24));
        }
        if (hours > 0) {
            return Lang.get("litebans_duration_hours_minutes")
                    .replace("{hours}", String.valueOf(hours))
                    .replace("{minutes}", String.valueOf(minutes % 60));
        }
        if (minutes > 0) {
            return Lang.get("litebans_duration_minutes")
                    .replace("{minutes}", String.valueOf(minutes));
        }
        return Lang.get("litebans_duration_seconds")
                .replace("{seconds}", String.valueOf(seconds));
    }

    private void debug(String key) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("[Debug] " + Lang.get(key));
        }
    }

    private void debug(String key, String value) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info(
                    "[Debug] " + Lang.get(key)
                            .replace("%value%", value)
                            .replace("%ticket%", value)
                            .replace("%channel%", value)
                            .replace("%role%", value)
            );
        }
    }

    private Long tryNumberGetter(Object obj, String method) {
        try {
            var m = obj.getClass().getMethod(method);
            Object r = m.invoke(obj);
            if (r instanceof Number n) {
                return n.longValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Boolean tryBoolGetter(Object obj, String method) {
        try {
            var m = obj.getClass().getMethod(method);
            Object r = m.invoke(obj);
            if (r instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private String tryStringGetter(Object obj, String method) {
        try {
            var m = obj.getClass().getMethod(method);
            Object r = m.invoke(obj);
            if (r != null) {
                return r.toString();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
