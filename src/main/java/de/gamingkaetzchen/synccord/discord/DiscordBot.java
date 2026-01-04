package de.gamingkaetzchen.synccord.discord;

import java.util.concurrent.RejectedExecutionException;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.commands.EmbitCommand;
import de.gamingkaetzchen.synccord.discord.commands.LinkMCCommand;
import de.gamingkaetzchen.synccord.discord.commands.SetupCommand;
import de.gamingkaetzchen.synccord.discord.commands.TicketSetupCommand;
import de.gamingkaetzchen.synccord.discord.commands.UnlinkMCCommand;
import de.gamingkaetzchen.synccord.discord.listener.DiscordChatListener;
import de.gamingkaetzchen.synccord.discord.listener.EmbitListener;
import de.gamingkaetzchen.synccord.discord.listener.InfoButtonListener;
import de.gamingkaetzchen.synccord.discord.listener.LinkHandler;
import de.gamingkaetzchen.synccord.discord.listener.MultiTicketSelectListener;
import de.gamingkaetzchen.synccord.discord.listener.RuleAcceptListener;
import de.gamingkaetzchen.synccord.discord.listener.TicketButtonListener;
import de.gamingkaetzchen.synccord.tickets.TicketManager;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBot {

    // nicht mehr final → damit Restart möglich wäre
    private JDA jda;
    private final TicketManager ticketManager;
    private final String token;

    public DiscordBot(String token, TicketManager ticketManager) throws Exception {
        this.ticketManager = ticketManager;
        this.token = token;

        debug("debug_discord_starting");
        connect(); // getrennt in eigene Methode
    }

    /**
     * Baut die JDA, wartet auf READY, registriert Listener & Commands
     */
    private void connect() throws Exception {
        JDABuilder builder = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT
                )
                // wieder aktivieren – JDA versucht dann selbstständig reconnects
                .setAutoReconnect(true);

        this.jda = builder.build();
        jda.awaitReady();

        // Listener registrieren
        jda.addEventListener(
                new EmbitCommand(),
                new EmbitListener(),
                new RuleAcceptListener(),
                new SetupCommand(), // /setup linking, info, playerlist, regel, multiticket
                new LinkHandler(),
                new InfoButtonListener(),
                new LinkMCCommand(),
                new UnlinkMCCommand(),
                new TicketButtonListener(ticketManager),
                new TicketSetupCommand(ticketManager, jda),
                new MultiTicketSelectListener(),
                new DiscordChatListener() // Discord → MC Chatbridge
        );

        debug("debug_discord_ready");

        registerCommands();

        // Info-Embed wiederherstellen/neu erstellen
        InfoUpdater.recoverOrOffline(jda);
    }

    private void registerCommands() {
        debug("debug_registering_slash_commands");

        // Option "type" für /setup – mit festen Choices
        net.dv8tion.jda.api.interactions.commands.build.OptionData setupTypeOption
                = new net.dv8tion.jda.api.interactions.commands.build.OptionData(
                        OptionType.STRING,
                        "type",
                        Lang.get("setup_option_type_description"),
                        true // required
                )
                        .addChoice("linking", "linking")
                        .addChoice("info", "info")
                        .addChoice("playerlist", "playerlist")
                        .addChoice("regel", "regel")
                        .addChoice("multiticket", "multiticket");

        jda.updateCommands().addCommands(
                // /setup mit optionalem Channel (für multiticket & playerlist)
                Commands.slash("setup", Lang.get("setup_description"))
                        .addOptions(setupTypeOption)
                        .addOption(OptionType.CHANNEL, "channel",
                                Lang.get("setup_option_channel_description"), false),
                Commands.slash("linkmc", Lang.get("linkmc_description"))
                        .addOption(OptionType.STRING, "uuid", Lang.get("linkmc_option_uuid"), true)
                        .addOption(OptionType.STRING, "discordid", Lang.get("linkmc_option_discordid"), true),
                Commands.slash("unlinkmc", Lang.get("unlinkmc_description"))
                        .addOption(OptionType.STRING, "uuid", Lang.get("unlinkmc_option_uuid"), true),
                Commands.slash("ticket", Lang.get("ticket_command_description"))
                        .addSubcommands(
                                new SubcommandData("setup", Lang.get("ticket_subcommand_setup_description"))
                                        .addOption(OptionType.STRING, "type",
                                                Lang.get("ticket_option_type_description"), true, true)
                                        .addOption(OptionType.CHANNEL, "channel",
                                                Lang.get("ticket_option_channel_description"), true)
                        ),
                // dein Embit-Command
                EmbitCommand.getCommandData()
        ).queue();
    }

    /**
     * Shutdown wird beim Plugin-Disable aufgerufen. Hier KEIN Restart – das
     * Plugin fährt ja bewusst runter.
     */
    public void shutdown() {
        if (jda != null) {
            debug("debug_discord_shutdown");
            try {
                jda.getPresence().setIdle(true);
            } catch (Exception ignored) {
            }
            jda.shutdownNow();
        }
    }

    public JDA getJDA() {
        return jda;
    }

    public TicketManager getTicketManager() {
        return ticketManager;
    }

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }

    private void debug(String key) {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(Lang.get(key));
        }
    }

    /**
     * Prüft, ob die JDA noch "lebendig" ist – hilfreich, wenn du irgendwann
     * einen echten Restart implementieren willst.
     */
    public boolean isAlive() {
        if (jda == null) {
            return false;
        }
        JDA.Status status = jda.getStatus();
        return status != JDA.Status.SHUTDOWN
                && status != JDA.Status.SHUTTING_DOWN
                && status != JDA.Status.DISCONNECTED;
    }

    /**
     * Einfaches Embed senden – aber MIT Statuscheck und Fehler-Handling, damit
     * keine RejectedExecutionException mehr fliegt.
     */
    public void sendSimpleEmbed(String channelId, String title, String description,
            java.awt.Color color, String thumbnailUrl) {

        if (jda == null) {
            return;
        }

        JDA.Status status = jda.getStatus();
        if (status == JDA.Status.SHUTTING_DOWN
                || status == JDA.Status.SHUTDOWN
                || status == JDA.Status.DISCONNECTED) {
            if (isDebug()) {
                Synccord.getInstance().getLogger().warning(
                        "[Synccord] JDA ist nicht verbunden – sendSimpleEmbed wird übersprungen."
                );
            }
            return;
        }

        MessageChannel channel = jda.getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            if (isDebug()) {
                Synccord.getInstance().getLogger().warning(
                        "[Synccord] sendSimpleEmbed: Channel nicht gefunden: " + channelId
                );
            }
            return;
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color);

        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            eb.setThumbnail(thumbnailUrl);
        }

        try {
            channel.sendMessageEmbeds(eb.build()).queue();
        } catch (RejectedExecutionException ex) {
            // Requester ist schon gestoppt → einfach nicht mehr senden
            Synccord.getInstance().getLogger().warning(
                    "[Synccord] Discord-Requester wurde gestoppt – Embed nicht gesendet: " + ex.getMessage()
            );
        } catch (Exception ex) {
            Synccord.getInstance().getLogger().warning(
                    "[Synccord] Fehler beim Senden eines Embeds: " + ex.getMessage()
            );
        }
    }

    /**
     * OPTIONAL: echter Restart, falls du ihn irgendwann aus einem Watchdog oder
     * Command aufrufen willst.
     *
     * Aktuell **nicht benutzt**, aber vorbereitet.
     */
    public void restart() {
        try {
            if (jda != null) {
                jda.shutdownNow();
            }
        } catch (Exception ignored) {
        }

        try {
            connect();
        } catch (Exception e) {
            Synccord.getInstance().getLogger().severe(
                    "[Synccord] Konnte Discord-Bot nach Restart nicht neu verbinden: " + e.getMessage()
            );
        }
    }
}
