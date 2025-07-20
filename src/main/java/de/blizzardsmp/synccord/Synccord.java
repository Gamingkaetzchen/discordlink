package de.blizzardsmp.synccord;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import de.blizzardsmp.synccord.commands.DcFindCommand;
import de.blizzardsmp.synccord.commands.UnlinkDiscordCommand;
import de.blizzardsmp.synccord.database.DatabaseManager;
import de.blizzardsmp.synccord.discord.DiscordBot;
import de.blizzardsmp.synccord.discord.InfoUpdaterOffline;
import de.blizzardsmp.synccord.listener.RoleSyncJoinListener;
import de.blizzardsmp.synccord.util.Lang;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class Synccord extends JavaPlugin {

    private static Synccord instance;
    private DiscordBot discordBot;
    private LuckPerms luckPerms;
    private boolean debug;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Debug-Flag aus config.yml lesen
        this.debug = getConfig().getBoolean("debug", false);

        // Sprache laden
        Lang.init();

        // bStats starten
        int pluginId = 26581;
        Metrics metrics = new Metrics(this, pluginId);

        // Datenbankverbindung initialisieren
        DatabaseManager.init();

        // Commands registrieren
        getCommand("unlinkdiscord").setExecutor(new UnlinkDiscordCommand());
        getCommand("dcfind").setExecutor(new DcFindCommand());

        // Event Listener registrieren
        Bukkit.getPluginManager().registerEvents(new RoleSyncJoinListener(), this);

        // LuckPerms initialisieren
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().warning("⚠ LuckPerms nicht verfügbar!");
        }

        // Discord-Bot starten
        try {
            discordBot = new DiscordBot(getConfig().getString("discord.token"));
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
}
