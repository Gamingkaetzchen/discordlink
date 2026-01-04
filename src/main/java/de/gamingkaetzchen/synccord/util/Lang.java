package de.gamingkaetzchen.synccord.util;

import java.io.File;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import de.gamingkaetzchen.synccord.Synccord;
import me.clip.placeholderapi.PlaceholderAPI;

public class Lang {

    private static YamlConfiguration langFile;
    private static YamlConfiguration fallbackFile;

    public static void init() {
        Synccord plugin = Synccord.getInstance();

        String langCode = plugin.getConfig().getString("language", "en");
        File langDir = new File(plugin.getDataFolder(), "lang");

        if (!langDir.exists() && !langDir.mkdirs()) {
            Bukkit.getLogger().warning("[Synccord] Could not create lang directory: " + langDir.getAbsolutePath());
        }

        // Hauptsprache
        File langFilePath = new File(langDir, langCode + ".yml");
        // Fallback immer EN
        File fallbackPath = new File(langDir, "en.yml");

        // Sicherstellen, dass en.yml + de.yml im Plugin-Ordner landen
        if (!fallbackPath.exists()) {
            plugin.saveResource("lang/en.yml", false);
            if (plugin.getResource("lang/de.yml") != null) {
                plugin.saveResource("lang/de.yml", false);
            }
        }

        // Ausgewählte Sprache nur dann kopieren, wenn sie auch im JAR existiert
        if (!langFilePath.exists()) {
            if (plugin.getResource("lang/" + langCode + ".yml") != null) {
                plugin.saveResource("lang/" + langCode + ".yml", false);
            } else {
                Bukkit.getLogger().warning("[Synccord] Language '" + langCode
                        + "' not found in plugin resources. Falling back to 'en'.");
                langCode = "en";
                langFilePath = fallbackPath;
            }
        }

        langFile = YamlConfiguration.loadConfiguration(langFilePath);
        fallbackFile = YamlConfiguration.loadConfiguration(fallbackPath);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Synccord] Lang initialized with language '" + langCode + "'.");
        }
    }

    public static String get(String key) {
        if (key == null || key.isEmpty()) {
            return "§c[Invalid lang key]";
        }

        // Sonderfall Prefix: NIE erneut %prefix% ersetzen, um Rekursion zu vermeiden
        if (key.equals("prefix")) {
            String rawPrefix = getRaw("prefix");
            return rawPrefix != null ? rawPrefix : "§7[§bSynccord§7] ";
        }

        String msg = getRaw(key);

        if (msg == null) {
            if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
                Bukkit.getLogger().warning("⚠ [Synccord] Sprachschlüssel fehlt: " + key);
            }
            return "§c[Missing lang key: " + key + "]";
        }

        // %prefix% in allen Messages ersetzen
        return msg.replace("%prefix%", get("prefix"));
    }

    /**
     * Holt den Wert aus der Hauptsprache oder dem Fallback, ohne
     * Prefix-Handling.
     */
    private static String getRaw(String key) {
        if (langFile == null || fallbackFile == null) {
            // Falls jemand vor init() etwas abfragt
            return "§c[Lang not initialized: " + key + "]";
        }

        String msg = langFile.getString(key);
        if (msg == null) {
            msg = fallbackFile.getString(key);
        }
        return msg;
    }

    public static String get(String key, Map<String, String> placeholders) {
        String msg = get(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return msg;
    }

    /**
     * Lang-Nachricht + PlaceholderAPI mit Spieler-Kontext.
     */
    public static String get(Player player, String key) {
        String msg = get(key);
        return applyPapi(player, key, msg);
    }

    /**
     * Lang-Nachricht + eigene Platzhalter + PlaceholderAPI mit Spieler-Kontext.
     */
    public static String get(Player player, String key, Map<String, String> placeholders) {
        String msg = get(key, placeholders);
        return applyPapi(player, key, msg);
    }

    private static String applyPapi(Player player, String key, String msg) {
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                msg = PlaceholderAPI.setPlaceholders(player, msg);
            } catch (Throwable t) {
                if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
                    Synccord.getInstance().getLogger().warning(
                            "[Synccord] Fehler bei PlaceholderAPI für key '" + key + "': " + t.getMessage()
                    );
                }
            }
        }
        return msg;
    }
}
