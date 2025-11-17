package de.gamingkaetzchen.synccord;

import java.io.File;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import de.gamingkaetzchen.synccord.commands.DcFindCommand;
import de.gamingkaetzchen.synccord.commands.UnlinkDiscordCommand;
import de.gamingkaetzchen.synccord.database.DatabaseManager;
import de.gamingkaetzchen.synccord.discord.DiscordBot;
import de.gamingkaetzchen.synccord.discord.InfoUpdaterOffline;
import de.gamingkaetzchen.synccord.listener.DiscordJoinLeaveForwardListener;
import de.gamingkaetzchen.synccord.listener.MinecraftChatListener;
import de.gamingkaetzchen.synccord.listener.PlayerActivityListener;
import de.gamingkaetzchen.synccord.listener.RoleSyncJoinListener;
import de.gamingkaetzchen.synccord.listener.TicketJoinAlertListener;
import de.gamingkaetzchen.synccord.tickets.TicketManager;
import de.gamingkaetzchen.synccord.util.Lang;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class Synccord extends JavaPlugin {

    private static Synccord instance;
    private DiscordBot discordBot;
    private LuckPerms luckPerms;
    private boolean debug;
    private TicketManager ticketManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.debug = getConfig().getBoolean("debug", false);

        // Sprache laden
        Lang.init();

        // bStats starten
        new Metrics(this, 26581);

        // Datenbank
        DatabaseManager.init();

        // TicketManager vor dem Bot!
        ticketManager = new TicketManager(this);

        // Befehle
        getCommand("unlinkdiscord").setExecutor(new UnlinkDiscordCommand());
        getCommand("dcfind").setExecutor(new DcFindCommand());

        // Listener
        getServer().getPluginManager().registerEvents(new TicketJoinAlertListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerActivityListener(), this);
        getServer().getPluginManager().registerEvents(new RoleSyncJoinListener(), this);
        getServer().getPluginManager().registerEvents(new DiscordJoinLeaveForwardListener(), this);
        getServer().getPluginManager().registerEvents(new MinecraftChatListener(), this);

        // rules.yml erzeugen
        File ruleFile = new File(getDataFolder(), "rules.yml");
        if (!ruleFile.exists()) {
            saveResource("rules.yml", false);
        }

        // LuckPerms
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().warning(Lang.get("debug_luckperms_missing"));
        }

        // LiteBans Info
        if (Bukkit.getPluginManager().getPlugin("LiteBans") != null) {
            getLogger().info(Lang.get("debug_litebans_found"));
        } else {
            getLogger().info(Lang.get("debug_litebans_not_found"));
        }

        // DiscordBot starten
        try {
            discordBot = new DiscordBot(getConfig().getString("discord.token"), ticketManager);
        } catch (Exception e) {
            getLogger().severe(Lang.get("error_discord_start"));
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        InfoUpdaterOffline.sendOfflineEmbedSync();

        if (discordBot != null) {
            discordBot.shutdown();
        }
    }

    public static Synccord getInstance() {
        return instance;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public boolean isDebug() {
        return debug;
    }

    public static void debug(String message) {
        if (getInstance().isDebug()) {
            getInstance().getLogger().info(
                    Lang.get("debug_generic").replace("%msg%", message)
            );
        }
    }

    public TicketManager getTicketManager() {
        return ticketManager;
    }
}
