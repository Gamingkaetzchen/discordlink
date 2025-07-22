package de.gamingkaetzchen.synccord.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;

public class DatabaseManager {

    private static Connection connection;

    public static void init() {
        try {
            File dbFile = new File(Synccord.getInstance().getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS linked_users ("
                    + "uuid TEXT PRIMARY KEY,"
                    + "discord_id TEXT NOT NULL,"
                    + "linked_at INTEGER"
                    + ");");
            stmt.close();

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(Lang.get("debug_sqlite_initialized"));
            }

        } catch (SQLException e) {
            Synccord.getInstance().getLogger().severe(Lang.get("sqlite_init_error"));
            e.printStackTrace();
        }
    }

    public static void link(UUID uuid, String discordId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO linked_users (uuid, discord_id, linked_at) VALUES (?, ?, ?);"
            );
            ps.setString(1, uuid.toString());
            ps.setString(2, discordId);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            ps.close();

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(
                        Lang.get("debug_sqlite_link_saved")
                                .replace("%uuid%", uuid.toString())
                                .replace("%discord%", discordId)
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Optional<String> getDiscordId(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT discord_id FROM linked_users WHERE uuid = ?;"
            );
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String discordId = rs.getString("discord_id");

                if (isDebug()) {
                    Synccord.getInstance().getLogger().info(
                            Lang.get("debug_sqlite_found_discord")
                                    .replace("%uuid%", uuid.toString())
                                    .replace("%discord%", discordId)
                    );
                }

                return Optional.ofNullable(discordId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get("debug_sqlite_discord_not_found")
                            .replace("%uuid%", uuid.toString())
            );
        }

        return Optional.empty();
    }

    public static boolean isLinked(UUID uuid) {
        boolean linked = getDiscordId(uuid).isPresent();
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get("debug_sqlite_islinked")
                            .replace("%uuid%", uuid.toString())
                            .replace("%linked%", String.valueOf(linked))
            );
        }
        return linked;
    }

    public static boolean isDiscordLinked(String discordId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid FROM linked_users WHERE discord_id = ?;"
            );
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(
                        Lang.get("debug_sqlite_isdiscordlinked")
                                .replace("%discord%", discordId)
                                .replace("%linked%", String.valueOf(exists))
                );
            }

            return exists;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void unlink(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM linked_users WHERE uuid = ?;"
            );
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
            ps.close();

            if (isDebug()) {
                Synccord.getInstance().getLogger().info(
                        Lang.get("debug_sqlite_link_deleted").replace("%uuid%", uuid.toString())
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }
}
