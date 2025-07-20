package de.blizzardsmp.synccord.listener;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import de.blizzardsmp.synccord.Synccord;
import de.blizzardsmp.synccord.discord.LinkManager;
import de.blizzardsmp.synccord.discord.RoleSyncUtil;
import de.blizzardsmp.synccord.util.Lang;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class RoleSyncJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Spieler nicht verlinkt → Kick mit Code
        if (!LinkManager.isLinked(uuid)) {
            String code = LinkManager.generateCodeFor(uuid);
            String rawMessage = Lang.get("login_kick_message")
                    .replace("%code%", code)
                    .replace("%prefix%", "");

            Component kickMessage = MiniMessage.miniMessage().deserialize(rawMessage);
            player.kick(kickMessage);
            debugLog(Lang.get("debug_kick_unlinked").replace("%name%", player.getName()));
            return;
        }

        // Discord-ID aus Datenbank holen
        Optional<String> optionalDiscordId = LinkManager.getDiscordId(uuid);
        if (optionalDiscordId.isEmpty()) {
            debugLog(Lang.get("debug_no_discord_id").replace("%name%", player.getName()));
            return;
        }

        String discordId = optionalDiscordId.get();

        // Guild-Objekt laden
        String guildId = Synccord.getInstance().getConfig().getString("discord.guild-id");
        if (guildId == null) {
            Synccord.getInstance().getLogger().warning(Lang.get("error_no_guild_id"));
            return;
        }

        Guild guild = Synccord.getInstance().getDiscordBot().getJDA().getGuildById(guildId);
        if (guild == null) {
            Synccord.getInstance().getLogger().warning(Lang.get("error_guild_not_found").replace("%id%", guildId));
            return;
        }

        // Discord-Mitglied abrufen & Rollen synchronisieren
        guild.retrieveMemberById(discordId).queue(
                (Member member) -> {
                    debugLog(Lang.get("debug_sync_start").replace("%name%", player.getName()));
                    RoleSyncUtil.syncRolesToMinecraft(member, uuid);
                },
                (error) -> Synccord.getInstance().getLogger().warning(Lang.get("error_discord_member").replace("%msg%", error.getMessage()))
        );
    }

    private void debugLog(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
