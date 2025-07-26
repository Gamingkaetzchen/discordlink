package de.gamingkaetzchen.synccord;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import de.gamingkaetzchen.synccord.commands.DcFindCommand;
import de.gamingkaetzchen.synccord.commands.UnlinkDiscordCommand;
import de.gamingkaetzchen.synccord.database.DatabaseManager;
import de.gamingkaetzchen.synccord.discord.DiscordBot;
import de.gamingkaetzchen.synccord.discord.InfoUpdaterOffline;
import de.gamingkaetzchen.synccord.listener.RoleSyncJoinListener;
import de.gamingkaetzchen.synccord.util.Lang;
import de.gamingkaetzchen.synccord.tickets.TicketManager;
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
        Metrics metrics = new Metrics(this, 26581);

        // Datenbankverbindung initialisieren
        DatabaseManager.init();

        // ✅ TicketManager initialisieren — VOR DiscordBot!
        ticketManager = new TicketManager(this);

        // Befehle registrieren
        getCommand("unlinkdiscord").setExecutor(new UnlinkDiscordCommand());
        getCommand("dcfind").setExecutor(new DcFindCommand());

        // Event-Listener
        Bukkit.getPluginManager().registerEvents(new RoleSyncJoinListener(), this);

        // LuckPerms
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().warning("⚠ LuckPerms nicht verfügbar!");
        }

        // ✅ Jetzt DiscordBot starten
        try {
            discordBot = new DiscordBot(getConfig().getString("discord.token"), ticketManager);
        } catch (Exception e) {
            getLogger().severe("❌ Fehler beim Starten des Discord-Bots:");
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
            getInstance().getLogger().info("§8[§3Debug§8] §7" + message);
        }
    }

    public TicketManager getTicketManager() {
        return ticketManager;
    }
}
