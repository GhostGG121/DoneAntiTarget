package com.done.antitarget;

import com.done.antitarget.command.AntiTargetCommand;
import com.done.antitarget.check.AutoFireworkCheck;
import com.done.antitarget.check.ElytraTargetCheck;
import com.done.antitarget.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class DoneAntiTarget extends JavaPlugin {

    private static DoneAntiTarget instance;
    private ConfigManager configManager;
    private AutoFireworkCheck autoFireworkCheck;
    private ElytraTargetCheck elytraTargetCheck;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        autoFireworkCheck = new AutoFireworkCheck(this);
        elytraTargetCheck = new ElytraTargetCheck(this);
        
        getServer().getPluginManager().registerEvents(autoFireworkCheck, this);
        getServer().getPluginManager().registerEvents(elytraTargetCheck, this);

        if (getCommand("doneantitarget") != null) {
            getCommand("doneantitarget").setExecutor(new AntiTargetCommand(this));
        }

        getLogger().info("DoneAntiTarget has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (autoFireworkCheck != null) {
            autoFireworkCheck.cleanup();
        }
        if (elytraTargetCheck != null) {
            elytraTargetCheck.cleanup();
        }
        getLogger().info("DoneAntiTarget has been disabled!");
    }

    public void reportViolation(Player player, String checkName, String info, int violations, int threshold) {
        if (configManager.isConsoleLogEnabled()) {
            String msg = configManager.getConsoleLogMessage()
                    .replace("%player%", player.getName())
                    .replace("%check%", checkName)
                    .replace("%info%", info)
                    .replace("%vl%", String.valueOf(violations))
                    .replace("%threshold%", String.valueOf(threshold));
            getLogger().warning(msg);
        }

        if (configManager.isAlertEnabled()) {
            String msgStr = configManager.getAlertMessage()
                    .replace("%player%", player.getName())
                    .replace("%check%", checkName)
                    .replace("%info%", info)
                    .replace("%vl%", String.valueOf(violations))
                    .replace("%threshold%", String.valueOf(threshold));
            Component alertComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(msgStr);

            for (Player onlinePlayer : getServer().getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("doneantitarget.alerts")) {
                    onlinePlayer.sendMessage(alertComponent);
                }
            }
        }

        if (configManager.isKickEnabled() && violations >= threshold) {
            String kickReasonStr = configManager.getKickReason()
                    .replace("%player%", player.getName())
                    .replace("%check%", checkName)
                    .replace("%info%", info)
                    .replace("%vl%", String.valueOf(violations))
                    .replace("%threshold%", String.valueOf(threshold));
            Component kickReasonComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(kickReasonStr);

            getServer().getScheduler().runTask(this, () -> {
                player.kick(kickReasonComponent);
            });
        }
    }

    public static DoneAntiTarget getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AutoFireworkCheck getAutoFireworkCheck() {
        return autoFireworkCheck;
    }

    public ElytraTargetCheck getElytraTargetCheck() {
        return elytraTargetCheck;
    }
}
