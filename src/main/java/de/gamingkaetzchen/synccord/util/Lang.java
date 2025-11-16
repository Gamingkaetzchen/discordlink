package de.gamingkaetzchen.synccord.util;

import java.io.File;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import de.gamingkaetzchen.synccord.Synccord;

public class Lang {

    private static YamlConfiguration langFile;
    private static YamlConfiguration fallbackFile;

    public static void init() {
        String lang = Synccord.getInstance().getConfig().getString("language", "en");
        File langDir = new File(Synccord.getInstance().getDataFolder(), "lang");

        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        File langFilePath = new File(langDir, lang + ".yml");
        File fallbackPath = new File(langDir, "en.yml");

        if (!langFilePath.exists()) {
            Synccord.getInstance().saveResource("lang/" + lang + ".yml", false);
        }

        if (!fallbackPath.exists()) {
            Synccord.getInstance().saveResource("lang/en.yml", false);
            Synccord.getInstance().saveResource("lang/de.yml", false);
        }

        langFile = YamlConfiguration.loadConfiguration(langFilePath);
        fallbackFile = YamlConfiguration.loadConfiguration(fallbackPath);
    }

    public static String get(String key) {
        if (key == null || key.isEmpty()) {
            return "§c[Invalid lang key]";
        }

        // Sonderfall Prefix
        if (key.equals("prefix")) {
            String rawPrefix = langFile.getString("prefix", fallbackFile.getString("prefix"));
            return rawPrefix != null ? rawPrefix : "§7[§bSynccord§7] ";
        }

        // Standardnachricht
        String msg = langFile.getString(key);
        if (msg == null) {
            msg = fallbackFile.getString(key);
        }

        if (msg == null) {
            if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
                Bukkit.getLogger().warning("⚠ [Synccord] Sprachschlüssel fehlt: " + key);
            }
            return "§c[Missing lang key: " + key + "]";
        }

        return msg.replace("%prefix%", get("prefix"));
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
}
