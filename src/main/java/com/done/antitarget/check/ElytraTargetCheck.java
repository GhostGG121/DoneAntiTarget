package com.done.antitarget.check;

import com.done.antitarget.DoneAntiTarget;
import com.done.antitarget.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ElytraTargetCheck implements Listener {

    private final DoneAntiTarget plugin;
    private final ConfigManager config;

    private final Map<UUID, PlayerRotationData> rotationDataMap;
    private final Map<UUID, PlayerLockData> lockDataMap;
    private final Map<UUID, PlayerSnapData> snapDataMap;
    private final Map<UUID, PlayerNoLookData> noLookDataMap;

    public ElytraTargetCheck(DoneAntiTarget plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.rotationDataMap = new ConcurrentHashMap<>();
        this.lockDataMap = new ConcurrentHashMap<>();
        this.snapDataMap = new ConcurrentHashMap<>();
        this.noLookDataMap = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (config.isCheckOnlyGliding() && !player.isGliding()) {
            rotationDataMap.remove(player.getUniqueId());
            PlayerSnapData snapData = snapDataMap.get(player.getUniqueId());
            if (snapData != null) {
                snapData.lastFireworkUse = -1;
            }
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getYaw() == to.getYaw() && from.getPitch() == to.getPitch()) {
            return;
        }

        float yawDiff = Math.abs(to.getYaw() - from.getYaw()) % 360;
        if (yawDiff > 180) {
            yawDiff = 360 - yawDiff;
        }
        float pitchDiff = Math.abs(to.getPitch() - from.getPitch());

        if (config.isRotationConsistencyEnabled() && (yawDiff >= 0.01 || pitchDiff >= 0.01)) {
            processRotationConsistency(player, yawDiff, pitchDiff);
        }

        if (config.isSnapToTargetEnabled()) {
            processSnapToTarget(player, yawDiff, pitchDiff);
        }
    }

    private void processRotationConsistency(Player player, float yawDiff, float pitchDiff) {
        PlayerRotationData rotData = rotationDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerRotationData());

        rotData.yawDeltas.add((double) yawDiff);
        rotData.pitchDeltas.add((double) pitchDiff);

        int sampleSize = config.getRotationConsistencySampleSize();
        if (rotData.yawDeltas.size() > sampleSize) {
            rotData.yawDeltas.poll();
        }
        if (rotData.pitchDeltas.size() > sampleSize) {
            rotData.pitchDeltas.poll();
        }

        if (rotData.yawDeltas.size() == sampleSize && rotData.pitchDeltas.size() == sampleSize) {
            double yawMean = calculateMean(rotData.yawDeltas);
            double pitchMean = calculateMean(rotData.pitchDeltas);

            double yawStdDev = calculateStdDev(rotData.yawDeltas);
            double pitchStdDev = calculateStdDev(rotData.pitchDeltas);

            boolean flagged = false;
            double maxStdDev = config.getRotationConsistencyMaxStdDev();
            double minMean = config.getRotationConsistencyMinMeanDelta();

            if (yawMean > minMean && yawStdDev < maxStdDev) {
                handleRotationConsistencyViolation(player, rotData, yawStdDev, true);
                flagged = true;
            }
            if (pitchMean > minMean && pitchStdDev < maxStdDev && !flagged) {
                handleRotationConsistencyViolation(player, rotData, pitchStdDev, false);
                flagged = true;
            }

            if (flagged) {
                rotData.yawDeltas.clear();
                rotData.pitchDeltas.clear();
            }
        }
    }

    private void processSnapToTarget(Player player, float yawDiff, float pitchDiff) {
        PlayerSnapData snapData = snapDataMap.get(player.getUniqueId());
        if (snapData == null || snapData.lastFireworkUse == -1) {
            return;
        }

        long now = System.currentTimeMillis();
        long windowMs = config.getSnapToTargetWatchTicks() * 50L;

        if (now - snapData.lastFireworkUse > windowMs) {
            snapData.lastFireworkUse = -1;
            return;
        }

        double rotationChange = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        if (rotationChange >= config.getSnapToTargetMinSnapAngle()) {
            double maxDist = config.getSnapToTargetMaxDistance();
            Entity nearest = null;
            double minAngle = Double.MAX_VALUE;

            Location eyeLoc = player.getEyeLocation();
            Vector eyeDir = eyeLoc.getDirection().normalize();

            for (Entity entity : player.getNearbyEntities(maxDist, maxDist, maxDist)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity living = (LivingEntity) entity;
                    Vector targetCenter = living.getBoundingBox().getCenter();
                    Vector toTarget = targetCenter.clone().subtract(eyeLoc.toVector()).normalize();

                    double dot = eyeDir.dot(toTarget);
                    double angleDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));

                    if (angleDeg < minAngle) {
                        minAngle = angleDeg;
                        nearest = living;
                    }
                }
            }

            if (nearest != null && minAngle <= config.getSnapToTargetMaxLockAngle()) {
                handleSnapToTargetViolation(player, snapData, rotationChange, minAngle, nearest.getName());
                snapData.lastFireworkUse = -1;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.isSnapToTargetEnabled()) {
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

        PlayerSnapData snapData = snapDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerSnapData());
        snapData.lastFireworkUse = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!config.isCenterMassLockEnabled() && !config.isNoLookHitEnabled()) {
            return;
        }

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        if (config.isCheckOnlyGliding() && !player.isGliding()) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity target = (LivingEntity) event.getEntity();

        Location eyeLoc = player.getEyeLocation();
        Vector eyeDir = eyeLoc.getDirection().normalize();
        Vector targetCenter = target.getBoundingBox().getCenter();
        Vector toTarget = targetCenter.clone().subtract(eyeLoc.toVector()).normalize();

        double dot = eyeDir.dot(toTarget);
        double angleDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));

        if (config.isCenterMassLockEnabled()) {
            PlayerLockData lockData = lockDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerLockData());

            if (angleDeg <= config.getCenterMassLockMaxAngle()) {
                lockData.consecutiveHits++;
                if (lockData.consecutiveHits >= config.getCenterMassLockConsecutiveHits()) {
                    handleCenterMassLockViolation(player, lockData, angleDeg);
                    lockData.consecutiveHits = 0;
                }
            } else {
                lockData.consecutiveHits = 0;
            }
        }

        if (config.isNoLookHitEnabled()) {
            if (angleDeg > config.getNoLookHitMaxAngle()) {
                PlayerNoLookData noLookData = noLookDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerNoLookData());
                handleNoLookHitViolation(player, noLookData, angleDeg, target.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        rotationDataMap.remove(uuid);
        lockDataMap.remove(uuid);
        snapDataMap.remove(uuid);
        noLookDataMap.remove(uuid);
    }

    private void handleRotationConsistencyViolation(Player player, PlayerRotationData data, double stdDev, boolean isYaw) {
        long now = System.currentTimeMillis();
        long decayTimeMs = config.getDecayTimeMs();

        if (data.lastViolationTime > 0) {
            long elapsed = now - data.lastViolationTime;
            int decay = (int) (elapsed / decayTimeMs);
            if (decay > 0) {
                data.violations = Math.max(0, data.violations - decay);
                data.lastViolationTime = now - (elapsed % decayTimeMs);
            }
        } else {
            data.lastViolationTime = now;
        }

        data.violations++;
        data.lastViolationTime = now;

        String axis = isYaw ? "Yaw" : "Pitch";
        plugin.reportViolation(player, "RotationConsistency", String.format("%s StdDev: %.6f", axis, stdDev), data.violations, config.getRotationConsistencyThreshold());
    }

    private void handleCenterMassLockViolation(Player player, PlayerLockData data, double angle) {
        long now = System.currentTimeMillis();
        long decayTimeMs = config.getDecayTimeMs();

        if (data.lastViolationTime > 0) {
            long elapsed = now - data.lastViolationTime;
            int decay = (int) (elapsed / decayTimeMs);
            if (decay > 0) {
                data.violations = Math.max(0, data.violations - decay);
                data.lastViolationTime = now - (elapsed % decayTimeMs);
            }
        } else {
            data.lastViolationTime = now;
        }

        data.violations++;
        data.lastViolationTime = now;

        plugin.reportViolation(player, "CenterMassLock", String.format("Dev: %.6f deg", angle), data.violations, config.getCenterMassLockThreshold());
    }

    private void handleSnapToTargetViolation(Player player, PlayerSnapData data, double snapAngle, double lockAngle, String targetName) {
        long now = System.currentTimeMillis();
        long decayTimeMs = config.getDecayTimeMs();

        if (data.lastViolationTime > 0) {
            long elapsed = now - data.lastViolationTime;
            int decay = (int) (elapsed / decayTimeMs);
            if (decay > 0) {
                data.violations = Math.max(0, data.violations - decay);
                data.lastViolationTime = now - (elapsed % decayTimeMs);
            }
        } else {
            data.lastViolationTime = now;
        }

        data.violations++;
        data.lastViolationTime = now;

        plugin.reportViolation(player, "SnapToTarget", String.format("Snap: %.1f deg, Lock: %.2f deg onto %s", snapAngle, lockAngle, targetName), data.violations, config.getSnapToTargetThreshold());
    }

    private void handleNoLookHitViolation(Player player, PlayerNoLookData data, double angle, String targetName) {
        long now = System.currentTimeMillis();
        long decayTimeMs = config.getDecayTimeMs();

        if (data.lastViolationTime > 0) {
            long elapsed = now - data.lastViolationTime;
            int decay = (int) (elapsed / decayTimeMs);
            if (decay > 0) {
                data.violations = Math.max(0, data.violations - decay);
                data.lastViolationTime = now - (elapsed % decayTimeMs);
            }
        } else {
            data.lastViolationTime = now;
        }

        data.violations++;
        data.lastViolationTime = now;

        plugin.reportViolation(player, "NoLookHit", String.format("Angle: %.1f deg hitting %s", angle, targetName), data.violations, config.getNoLookHitThreshold());
    }

    public void cleanup() {
        rotationDataMap.clear();
        lockDataMap.clear();
        snapDataMap.clear();
        noLookDataMap.clear();
    }

    private double calculateMean(Queue<Double> deltas) {
        double sum = 0;
        for (double d : deltas) {
            sum += d;
        }
        return sum / deltas.size();
    }

    private double calculateStdDev(Queue<Double> deltas) {
        double sum = 0;
        for (double d : deltas) {
            sum += d;
        }
        double mean = sum / deltas.size();
        double temp = 0;
        for (double d : deltas) {
            temp += (d - mean) * (d - mean);
        }
        double variance = temp / deltas.size();
        return Math.sqrt(variance);
    }

    private static class PlayerRotationData {
        private final Queue<Double> yawDeltas = new LinkedList<>();
        private final Queue<Double> pitchDeltas = new LinkedList<>();
        private int violations = 0;
        private long lastViolationTime = -1;
    }

    private static class PlayerLockData {
        private int consecutiveHits = 0;
        private int violations = 0;
        private long lastViolationTime = -1;
    }

    private static class PlayerSnapData {
        private long lastFireworkUse = -1;
        private int violations = 0;
        private long lastViolationTime = -1;
    }

    private static class PlayerNoLookData {
        private int violations = 0;
        private long lastViolationTime = -1;
    }
}
