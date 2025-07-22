package de.gamingkaetzchen.synccord.discord;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.discord.commands.LinkMCCommand;
import de.gamingkaetzchen.synccord.discord.commands.SetupCommand;
import de.gamingkaetzchen.synccord.discord.commands.UnlinkMCCommand;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBot {

    private final JDA jda;

    public DiscordBot(String token) throws Exception {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(Lang.get("debug_discord_starting"));
        }

        this.jda = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .addEventListeners(
                        new SetupCommand(),
                        new LinkHandler(),
                        new InfoButtonListener(),
                        new LinkMCCommand(),
                        new UnlinkMCCommand()
                )
                .build();

        jda.awaitReady();

        if (isDebug()) {
            Synccord.getInstance().getLogger().info(Lang.get("debug_discord_ready"));
        }

        registerCommands();
        InfoUpdater.recoverOrOffline(jda);
    }

    private void registerCommands() {
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(Lang.get("debug_registering_slash_commands"));
        }

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
                        .addOption(OptionType.STRING, "uuid", Lang.get("unlinkmc_option_uuid"), true)
        ).queue();
    }

    public void shutdown() {
        if (jda != null) {
            if (isDebug()) {
                Synccord.getInstance().getLogger().info(Lang.get("debug_discord_shutdown"));
            }
            jda.shutdown();
        }
    }

    public JDA getJDA() {
        return jda;
    }

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }
}
