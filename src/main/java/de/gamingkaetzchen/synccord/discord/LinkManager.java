package de.gamingkaetzchen.synccord.discord;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.database.DatabaseManager;
import de.gamingkaetzchen.synccord.util.Lang;

public class LinkManager {

    private static final Map<UUID, CodeEntry> activeCodes = new HashMap<>();
    private static final Map<String, UUID> reverseCodes = new HashMap<>();

    private record CodeEntry(String code, long expiresAt) {

    }

    public static boolean isLinked(UUID uuid) {
        return DatabaseManager.isLinked(uuid);
    }

    public static String generateCodeFor(UUID uuid) {
        long now = System.currentTimeMillis();
        CodeEntry existing = activeCodes.get(uuid);

        if (existing != null && existing.expiresAt > now) {
            debugLog(Lang.get("debug_link_code_still_valid")
                    .replace("%uuid%", uuid.toString())
                    .replace("%code%", existing.code));
            return existing.code;
        }

        String code = generateUniqueCode(6);
        long expiryMinutes = Synccord.getInstance().getConfig().getLong("linking.code-expiry-minutes", 0);
        long expiresAt = (expiryMinutes > 0) ? now + expiryMinutes * 60_000L : Long.MAX_VALUE;

        activeCodes.put(uuid, new CodeEntry(code, expiresAt));
        reverseCodes.put(code, uuid);

        debugLog(Lang.get("debug_link_code_generated")
                .replace("%uuid%", uuid.toString())
                .replace("%code%", code)
                .replace("%minutes%", String.valueOf(expiryMinutes)));

        return code;
    }

    public static UUID getUUIDByCode(String code) {
        UUID uuid = reverseCodes.get(code);
        if (uuid == null) {
            debugLog(Lang.get("debug_link_code_not_found").replace("%code%", code));
            return null;
        }

        CodeEntry entry = activeCodes.get(uuid);
        if (entry == null || System.currentTimeMillis() > entry.expiresAt) {
            debugLog(Lang.get("debug_link_code_expired").replace("%code%", code).replace("%uuid%", uuid.toString()));
            activeCodes.remove(uuid);
            reverseCodes.remove(code);
            return null;
        }

        debugLog(Lang.get("debug_link_code_valid").replace("%code%", code).replace("%uuid%", uuid.toString()));
        return uuid;
    }

    public static void link(UUID uuid, String discordId) {
        activeCodes.remove(uuid);
        reverseCodes.entrySet().removeIf(entry -> entry.getValue().equals(uuid));
        DatabaseManager.link(uuid, discordId);
        debugLog(Lang.get("debug_link_stored").replace("%uuid%", uuid.toString()).replace("%id%", discordId));
    }

    public static Optional<String> getDiscordId(UUID uuid) {
        return DatabaseManager.getDiscordId(uuid);
    }

    private static String generateUniqueCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        String code;

        do {
            sb.setLength(0);
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (reverseCodes.containsKey(code));

        return code;
    }

    private static void debugLog(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
