package com.done.antitarget.command;

import com.done.antitarget.DoneAntiTarget;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AntiTargetCommand implements CommandExecutor, TabCompleter {

    private final DoneAntiTarget plugin;

    public AntiTargetCommand(DoneAntiTarget plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("doneantitarget.admin")) {
            Component errorMsg = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&cDoneAntiTarget&8] &cYou do not have permission to execute this command!");
            sender.sendMessage(errorMsg);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().loadConfig();
            plugin.getAutoFireworkCheck().cleanup();
            if (plugin.getElytraTargetCheck() != null) {
                plugin.getElytraTargetCheck().cleanup();
            }
            Component successMsg = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&cDoneAntiTarget&8] &aConfiguration successfully reloaded and data caches cleared!");
            sender.sendMessage(successMsg);
            return true;
        }

        Component usageMsg = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&cDoneAntiTarget&8] &7Usage: &e/doneantitarget reload");
        sender.sendMessage(usageMsg);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender.hasPermission("doneantitarget.admin") && args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
