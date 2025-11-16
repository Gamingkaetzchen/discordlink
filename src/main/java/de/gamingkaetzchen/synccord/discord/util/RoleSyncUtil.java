package de.gamingkaetzchen.synccord.discord.util;

import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

public class RoleSyncUtil {

    public static void syncRolesToMinecraft(Member member, UUID uuid) {
        debugLog(Lang.get("sync_start").replace("%uuid%", uuid.toString()));

        ConfigurationSection discordSection = Synccord.getInstance().getConfig().getConfigurationSection("discord");
        if (discordSection == null) {
            Synccord.getInstance().getLogger().warning("⚠ 'discord' section not found in config.yml");
            return;
        }

        ConfigurationSection roleLinkSection = discordSection.getConfigurationSection("role-link");
        if (roleLinkSection == null) {
            Synccord.getInstance().getLogger().warning(Lang.get("sync_no_roles_config"));
            return;
        }

        Map<String, Object> roleMap = roleLinkSection.getValues(false);
        if (roleMap.isEmpty()) {
            Synccord.getInstance().getLogger().warning(Lang.get("sync_no_roles_config"));
            return;
        }

        LuckPerms lp = Synccord.getInstance().getLuckPerms();
        User user = lp.getUserManager().getUser(uuid);
        if (user == null) {
            Synccord.getInstance().getLogger().warning(Lang.get("sync_no_lp_user").replace("%uuid%", uuid.toString()));
            return;
        }

        debugLog(Lang.get("sync_discord_roles"));
        for (Role r : member.getRoles()) {
            debugLog(" - " + r.getName() + " (" + r.getId() + ")");
        }

        for (Map.Entry<String, Object> entry : roleMap.entrySet()) {
            String roleId = entry.getKey();
            String groupName = String.valueOf(entry.getValue());

            Role role = member.getGuild().getRoleById(roleId);
            if (role == null) {
                Synccord.getInstance().getLogger().warning(Lang.get("sync_role_not_found").replace("%role%", roleId));
                continue;
            }

            boolean hasDiscordRole = member.getRoles().contains(role);
            boolean hasGroup = user.getNodes().stream().anyMatch(n -> n.getKey().equals("group." + groupName));

            debugLog(Lang.get("sync_check")
                    .replace("%role%", role.getName())
                    .replace("%group%", groupName));
            debugLog(" → " + Lang.get("sync_has_role") + ": " + hasDiscordRole + ", " + Lang.get("sync_has_group") + ": " + hasGroup);

            Node node = Node.builder("group." + groupName).build();

            if (hasDiscordRole && !hasGroup) {
                lp.getUserManager().modifyUser(uuid, u -> u.data().add(node));
                debugLog(Lang.get("sync_added").replace("%group%", groupName));
            } else if (!hasDiscordRole && hasGroup) {
                lp.getUserManager().modifyUser(uuid, u -> u.data().remove(node));
                debugLog(Lang.get("sync_removed").replace("%group%", groupName));
            } else {
                debugLog(Lang.get("sync_no_change").replace("%group%", groupName));
            }
        }

        debugLog(Lang.get("sync_done"));
    }

    private static void debugLog(String msg) {
        if (Synccord.getInstance().getConfig().getBoolean("debug", false)) {
            Synccord.getInstance().getLogger().info("🪲 DEBUG | " + msg);
        }
    }
}
