package de.gamingkaetzchen.synccord.discord;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.commands.LinkMCCommand;
import de.gamingkaetzchen.synccord.discord.commands.SetupCommand;
import de.gamingkaetzchen.synccord.discord.commands.TicketSetupCommand;
import de.gamingkaetzchen.synccord.discord.commands.UnlinkMCCommand;
import de.gamingkaetzchen.synccord.discord.listener.TicketButtonListener;
import de.gamingkaetzchen.synccord.tickets.TicketManager;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBot {

    private final JDA jda;
    private final TicketManager ticketManager;

    public DiscordBot(String token, TicketManager ticketManager) throws Exception {
        this.ticketManager = ticketManager;

        debug("debug_discord_starting");

        JDABuilder builder = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT
                );

        this.jda = builder.build();
        jda.awaitReady();

        jda.addEventListener(
                new SetupCommand(),
                new LinkHandler(),
                new InfoButtonListener(),
                new LinkMCCommand(),
                new UnlinkMCCommand(),
                new TicketButtonListener(ticketManager),
                new TicketSetupCommand(ticketManager, jda)
        );

        debug("debug_discord_ready");

        registerCommands();
        InfoUpdater.recoverOrOffline(jda);
    }

    private void registerCommands() {
        debug("debug_registering_slash_commands");

        jda.updateCommands().addCommands(
                Commands.slash("setup", Lang.get("setup_description"))
                        .addOption(
                                OptionType.STRING,
                                "type",
                                Lang.get("setup_option_type_description"),
                                true,
                                true
                        ),
                Commands.slash("linkmc", Lang.get("linkmc_description"))
                        .addOption(OptionType.STRING, "uuid", Lang.get("linkmc_option_uuid"), true)
                        .addOption(OptionType.STRING, "discordid", Lang.get("linkmc_option_discordid"), true),
                Commands.slash("unlinkmc", Lang.get("unlinkmc_description"))
                        .addOption(OptionType.STRING, "uuid", Lang.get("unlinkmc_option_uuid"), true),
                Commands.slash("ticket", "Ticket-System verwalten")
                        .addSubcommands(
                                new SubcommandData("setup", "Ticket-Buttons posten")
                                        .addOption(OptionType.STRING, "type", "Tickettyp-ID aus config.yml", true, true)
                                        .addOption(OptionType.CHANNEL, "channel", "Channel, in dem der Button gepostet werden soll", true)
                        )
        ).queue();
    }

    public void shutdown() {
        if (jda != null) {
            debug("debug_discord_shutdown");
            jda.shutdown();
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
}
