package de.gamingkaetzchen.synccord.tickets;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class TicketManager {

    private final JavaPlugin plugin;
    private final Map<String, TicketType> ticketTypes = new HashMap<>();
    private final Map<Long, UUID> linkedPlayers = new HashMap<>(); // ✅ Discord-ID → Minecraft-UUID

    public TicketManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadTickets();
    }

    public void loadTickets() {
        ticketTypes.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("tickets");
        if (section == null) {
            // optionales Debug-Log, falls keine tickets-Sektion existiert
            if (isDebug()) {
                plugin.getLogger().warning(Lang.get("debug_ticket_no_config"));
            }
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection ticketSection = section.getConfigurationSection(key);
            if (ticketSection == null) {
                if (isDebug()) {
                    plugin.getLogger().warning(
                            Lang.get("debug_ticket_section_missing")
                                    .replace("%value%", key));
                }
                continue;
            }

            // Namen, Beschreibung, Button-Text – mit Lang-Fallback
            String name = ticketSection.getString("name", Lang.get("ticket_default_name"));
            String description = ticketSection.getString("description", "");
            String buttonName = ticketSection.getString("button_name",
                    Lang.get("ticket_default_button_name"));

            String categoryId = ticketSection.getString("ticketkategorie");
            List<String> supporterRoles = ticketSection.getStringList("supporter_roles");

            // litebans-hook aus config lesen (beide Schreibweisen unterstützen)
            boolean litebansHook = ticketSection.getBoolean("litebanshook",
                    ticketSection.getBoolean("litebans-hook", false));

            Map<Integer, TicketQuestion> questions = new TreeMap<>();
            ConfigurationSection questionSection = ticketSection.getConfigurationSection("question");
            if (questionSection != null) {
                for (String qKey : questionSection.getKeys(false)) {
                    try {
                        int index = Integer.parseInt(qKey);
                        int inputLimit = questionSection.getInt(qKey + ".input", 100);
                        List<String> questionLines = questionSection.getStringList(qKey + ".questions");
                        questions.put(index, new TicketQuestion(inputLimit, questionLines));
                    } catch (NumberFormatException ex) {
                        // Konfigurationsfehler → klares Log mit Lang
                        plugin.getLogger().warning(
                                Lang.get("ticket_invalid_question_index")
                                        .replace("%ticket%", key)
                                        .replace("%index%", qKey));
                    }
                }
            }

            TicketType ticketType = new TicketType(
                    key,
                    name,
                    description,
                    buttonName,
                    categoryId,
                    supporterRoles,
                    questions,
                    litebansHook
            );
            ticketTypes.put(key, ticketType);

            // Debug-Log pro geladenem Tickettyp
            if (isDebug()) {
                plugin.getLogger().info(
                        Lang.get("debug_ticket_type_loaded")
                                .replace("%value%", key));
            }
        }
    }

    public Collection<TicketType> getAllTicketTypes() {
        return ticketTypes.values();
    }

    public TicketType getTicketTypeById(String id) {
        return ticketTypes.get(id);
    }

    public TicketType getTypeByChannel(TextChannel channel) {
        String name = channel.getName();
        for (TicketType type : getTicketTypes()) {
            if (name.startsWith(type.getId() + "-")) {
                return type;
            }
        }
        return null;
    }

    public TicketType getTypeByChannelId(String channelId) {
        File file = new File("tickets", channelId + ".yml");
        if (!file.exists()) {
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String typeId = config.getString("ticket_id");
        if (typeId == null) {
            return null;
        }

        return getTicketTypeById(typeId);
    }

    public Optional<Player> getOnlinePlayer(long discordId) {
        UUID uuid = linkedPlayers.get(discordId);
        return uuid == null ? Optional.empty() : Optional.ofNullable(Bukkit.getPlayer(uuid));
    }

    public Optional<String> getName(long discordId) {
        UUID uuid = linkedPlayers.get(discordId);
        return uuid == null ? Optional.empty() : Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName());
    }

    public void linkPlayer(long discordId, UUID uuid) {
        linkedPlayers.put(discordId, uuid);
    }

    public void unlinkPlayer(long discordId) {
        linkedPlayers.remove(discordId);
    }

    public boolean isLinked(long discordId) {
        return linkedPlayers.containsKey(discordId);
    }

    public Map<Long, UUID> getLinkedPlayers() {
        return linkedPlayers;
    }

    public Collection<TicketType> getTicketTypes() {
        return ticketTypes.values();
    }

    private boolean isDebug() {
        return plugin.getConfig().getBoolean("debug", false);
    }
}
