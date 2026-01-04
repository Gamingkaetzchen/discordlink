package de.gamingkaetzchen.synccord.placeholder;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.InfoUpdater;
import de.gamingkaetzchen.synccord.discord.LinkManager;
import de.gamingkaetzchen.synccord.util.Lang;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class SynccordPlaceholder extends PlaceholderExpansion {

    private final Synccord plugin;
    private final DateTimeFormatter dateFormatter
            = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN)
                    .withZone(ZoneId.systemDefault());

    public SynccordPlaceholder(Synccord plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        // → %synccord_<placeholder>%
        return "synccord";
    }

    @Override
    public String getAuthor() {
        if (!plugin.getDescription().getAuthors().isEmpty()) {
            return String.join(", ", plugin.getDescription().getAuthors());
        }
        return "GamingKaetzchen";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        // Discord / Guild vorbereiten
        JDA jda = null;
        Guild guild = null;

        if (plugin.getDiscordBot() != null) {
            jda = plugin.getDiscordBot().getJDA();
            String guildId = plugin.getConfig().getString("discord.guild-id");
            if (jda != null && guildId != null && !guildId.isEmpty()) {
                guild = jda.getGuildById(guildId);
            }
        }

        String key = params.toLowerCase(Locale.ROOT);

        // ===================== GLOBALE PLACEHOLDER (ohne Player) =====================
        switch (key) {
            case "tps":
                return formatTps();

            case "mspt":
                return formatMspt();

            case "online_count":
                return String.valueOf(Bukkit.getOnlinePlayers().size());

            case "max_players":
                return String.valueOf(Bukkit.getMaxPlayers());

            case "uptime":
                return formatUptime();

            case "ip_java":
                return plugin.getConfig().getString(
                        "discord.java-ip",
                        Lang.get("placeholder_ip_unknown")
                );

            case "ip_bedrock":
                return plugin.getConfig().getString(
                        "discord.bedrock-ip",
                        Lang.get("placeholder_ip_unknown")
                );

            case "synccord_version":
                return plugin.getDescription().getVersion();

            case "status_embed_active":
                // Aktiv, wenn InfoUpdater schon einmal eine Nachricht gespeichert hat
                return (InfoUpdater.getLastChannelId() != null
                        && InfoUpdater.getLastMessageId() != null) ? "true" : "false";

            case "playerlist_embed_active":
                // Aktiv, wenn playerlist-state.yml mit channel+message existiert
                return isPlayerlistActive() ? "true" : "false";

            case "discord_online":
                return String.valueOf(countDiscordOnline(guild));

            case "linked_online":
                return String.valueOf(countLinkedOnline());

            case "tickets_open":
                // TicketManager tracked das aktuell noch nicht → vorerst 0
                return "0";
        }

        // Ab hier: alles Spielerbezogene → ohne Player macht es keinen Sinn
        if (player == null) {
            return "";
        }

        // Basis-Playerdaten (für %synccord_player%, %synccord_world%, %synccord_x% etc.)
        Location loc = player.getLocation();

        // ===================== BASIS-PLAYER-PLACEHOLDER =====================
        switch (key) {
            case "player":
                return player.getName();

            case "uuid":
                return player.getUniqueId().toString();

            case "world":
                return (loc.getWorld() != null) ? loc.getWorld().getName() : "";

            case "x":
                return String.valueOf(loc.getBlockX());

            case "y":
                return String.valueOf(loc.getBlockY());

            case "z":
                return String.valueOf(loc.getBlockZ());
        }

        // Discord-Verknüpfung per LinkManager + Datenbank
        String discordId = getDiscordIdForPlayer(player);
        Member member = (guild != null && discordId != null) ? guild.getMemberById(discordId) : null;

        // ===================== SPIELER-BEZOGENE DISCORD-PLACEHOLDER =====================
        switch (key) {
            // --- Link / Account ---
            case "discord_id":
                return discordId != null ? discordId : "";

            case "discord_linked":
                return discordId != null ? "true" : "false";

            case "discord_status":
                // Jetzt über Langfile
                if (discordId == null) {
                    return Lang.get("placeholder_discord_status_not_linked");
                } else {
                    return Lang.get("placeholder_discord_status_linked");
                }

            // --- Name / Avatar ---
            case "discord_tag":
                return member != null ? member.getUser().getAsTag() : "";

            case "discord_username":
                return member != null ? member.getUser().getName() : "";

            case "discord_avatar":
                return member != null ? member.getEffectiveAvatarUrl() : "";

            // --- Rollen ---
            case "discord_roles":
                if (member != null) {
                    return member.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.joining(", "));
                }
                return "";

            case "role_count":
                return (member != null) ? String.valueOf(member.getRoles().size()) : "0";

            case "highest_role":
                if (member != null && !member.getRoles().isEmpty()) {
                    // höchste Rolle (i. d. R. erste in der Liste)
                    return member.getRoles().get(0).getName();
                }
                return "";

            // --- Link-Meta (Platzhalter für spätere DB-Erweiterung) ---
            case "sync_fail_reason":
                return "";

            case "last_linked":
                return "";

            case "code_expire":
                return "";

            // --- Tickets pro Spieler (noch kein Tracking in TicketManager) ---
            case "tickets_user":
                return "0";

            case "ticket_support_role":
                return getTicketSupportRoleNames(guild);

            case "last_ticket_time":
                return "";
        }

        // ===================== PARAMETRISIERTE PLACEHOLDER =====================
        // %synccord_discord_role:<id>% → true/false
        if (key.startsWith("discord_role:")) {
            String roleId = key.substring("discord_role:".length());
            if (member != null && roleId.matches("\\d+")) {
                boolean hasRole = member.getRoles().stream()
                        .anyMatch(r -> r.getId().equals(roleId));
                return hasRole ? "true" : "false";
            }
            return "false";
        }

        return null;
    }

    // =====================================================================
    // Hilfsmethoden
    // =====================================================================
    private String getDiscordIdForPlayer(Player player) {
        try {
            Optional<String> opt = LinkManager.getDiscordId(player.getUniqueId());
            return opt.orElse(null);
        } catch (Exception ex) {
            if (plugin.isDebug()) {
                plugin.getLogger().warning(
                        Lang.get("debug_placeholder_discordid_failed")
                                .replace("%error%", ex.getMessage() == null ? "null" : ex.getMessage())
                );
            }
            return null;
        }
    }

    /**
     * Prüft playerlist-state.yml auf gültige channel-id / message-id. (Falls du
     * auf playerlist.yml umgestellt hast, hier ggf. den Dateinamen anpassen.)
     */
    private boolean isPlayerlistActive() {
        File file = new File(plugin.getDataFolder(), "playerlist-state.yml");
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        String ch = yml.getString("channel-id");
        String msg = yml.getString("message-id");
        return ch != null && !ch.isEmpty() && msg != null && !msg.isEmpty();
    }

    private int countDiscordOnline(Guild guild) {
        if (guild == null) {
            return 0;
        }
        return (int) guild.getMembers().stream()
                .filter(m -> m.getOnlineStatus() != OnlineStatus.OFFLINE
                && m.getOnlineStatus() != OnlineStatus.INVISIBLE)
                .count();
    }

    private int countLinkedOnline() {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            String id = getDiscordIdForPlayer(p);
            if (id != null) {
                count++;
            }
        }
        return count;
    }

    private String getTicketSupportRoleNames(Guild guild) {
        if (guild == null) {
            return "";
        }

        var cfg = plugin.getConfig();
        String path = "tickets.support.supporter_roles"; // dein Standard-Support-Typ
        if (!cfg.isList(path)) {
            return "";
        }

        return cfg.getStringList(path).stream()
                .map(id -> guild.getRoleById(id))
                .filter(r -> r != null)
                .map(Role::getName)
                .collect(Collectors.joining(", "));
    }

    private String formatTps() {
        try {
            double[] tps = Bukkit.getTPS();
            double current = tps.length > 0 ? tps[0] : 20.0;
            return String.format(Locale.US, "%.2f", current);
        } catch (NoSuchMethodError err) {
            return "20.00";
        }
    }

    private String formatMspt() {
        try {
            double mspt = Bukkit.getAverageTickTime();
            return String.format(Locale.US, "%.2f", mspt);
        } catch (NoSuchMethodError err) {
            return "-";
        }
    }

    private String formatUptime() {
        long start = plugin.getStartTimeMillis();
        long now = System.currentTimeMillis();
        long diff = now - start;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        long s = seconds % 60;
        long m = minutes % 60;
        long h = hours;

        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
