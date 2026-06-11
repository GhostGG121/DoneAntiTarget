package com.done.antitarget.check;

import com.done.antitarget.DoneAntiTarget;
import com.done.antitarget.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoFireworkCheck implements Listener {

    private final DoneAntiTarget plugin;
    private final ConfigManager config;
    private final Map<UUID, PlayerData> playerDataMap;

    public AutoFireworkCheck(DoneAntiTarget plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.playerDataMap = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        if (!config.isAutoFireworkEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        
        if (config.isCheckOnlyGliding() && !player.isGliding()) {
            PlayerData data = playerDataMap.get(player.getUniqueId());
            if (data != null) {
                data.resetSequence();
            }
            return;
        }

        int prev = event.getPreviousSlot();
        int next = event.getNewSlot();
        long now = System.currentTimeMillis();

        PlayerData data = playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData());

        if (data.originalSlot == -1) {
            data.originalSlot = prev;
            data.targetSlot = next;
            data.switchTime = now;
            data.fireworkUsed = false;
        } else {
            if (next == data.originalSlot) {
                if (data.fireworkUsed) {
                    long elapsed = now - data.switchTime;
                    if (elapsed <= config.getAutoFireworkMaxMs()) {
                        handleViolation(player, data, elapsed);
                    } else {
                        data.resetSequence();
                    }
                } else {
                    data.resetSequence();
                }
            } else {
                data.originalSlot = prev;
                data.targetSlot = next;
                data.switchTime = now;
                data.fireworkUsed = false;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (!config.isAutoFireworkEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        if (config.isCheckOnlyGliding() && !player.isGliding()) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET) {
            return;
        }

        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) {
            return;
        }

        int currentSlot = player.getInventory().getHeldItemSlot();
        if (data.originalSlot != -1 && currentSlot == data.targetSlot) {
            data.fireworkUsed = true;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataMap.remove(event.getPlayer().getUniqueId());
    }

    private void handleViolation(Player player, PlayerData data, long elapsed) {
        long now = System.currentTimeMillis();
        long decayTimeMs = config.getDecayTimeMs();

        if (data.lastViolationTime > 0) {
            long elapsedSinceLast = now - data.lastViolationTime;
            int decay = (int) (elapsedSinceLast / decayTimeMs);
            if (decay > 0) {
                data.violations = Math.max(0, data.violations - decay);
                data.lastViolationTime = now - (elapsedSinceLast % decayTimeMs);
            }
        } else {
            data.lastViolationTime = now;
        }

        data.violations++;
        data.lastViolationTime = now;

        int currentVL = data.violations;
        int threshold = config.getAutoFireworkThreshold();

        plugin.reportViolation(player, "AutoFirework", elapsed + "ms", currentVL, threshold);

        data.resetSequence();
    }

    public void cleanup() {
        playerDataMap.clear();
    }

    public void resetPlayer(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    private static class PlayerData {
        private int originalSlot = -1;
        private int targetSlot = -1;
        private long switchTime = -1;
        private boolean fireworkUsed = false;
        private int violations = 0;
        private long lastViolationTime = -1;

        public void resetSequence() {
            this.originalSlot = -1;
            this.targetSlot = -1;
            this.switchTime = -1;
            this.fireworkUsed = false;
        }
    }
}
