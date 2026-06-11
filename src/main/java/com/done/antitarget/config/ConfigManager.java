package com.done.antitarget.config;

import com.done.antitarget.DoneAntiTarget;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final DoneAntiTarget plugin;
    
    private long decayTimeMs;
    private boolean checkOnlyGliding;
    
    private boolean autoFireworkEnabled;
    private long autoFireworkMaxMs;
    private int autoFireworkThreshold;

    private boolean rotationConsistencyEnabled;
    private int rotationConsistencySampleSize;
    private double rotationConsistencyMaxStdDev;
    private double rotationConsistencyMinMeanDelta;
    private int rotationConsistencyThreshold;

    private boolean centerMassLockEnabled;
    private double centerMassLockMaxAngle;
    private int centerMassLockConsecutiveHits;
    private int centerMassLockThreshold;

    private boolean snapToTargetEnabled;
    private int snapToTargetWatchTicks;
    private double snapToTargetMinSnapAngle;
    private double snapToTargetMaxLockAngle;
    private double snapToTargetMaxDistance;
    private int snapToTargetThreshold;

    private boolean noLookHitEnabled;
    private double noLookHitMaxAngle;
    private int noLookHitThreshold;
    
    private boolean kickEnabled;
    private String kickReason;
    
    private boolean alertEnabled;
    private String alertMessage;
    
    private boolean consoleLogEnabled;
    private String consoleLogMessage;

    public ConfigManager(DoneAntiTarget plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.decayTimeMs = config.getLong("detection.decay-time-seconds", 30) * 1000L;
        this.checkOnlyGliding = config.getBoolean("detection.check-only-gliding", true);

        this.autoFireworkEnabled = config.getBoolean("detection.auto-firework.enabled", true);
        this.autoFireworkMaxMs = config.getLong("detection.auto-firework.max-ms-threshold", 150);
        this.autoFireworkThreshold = config.getInt("detection.auto-firework.violation-threshold", 3);

        this.rotationConsistencyEnabled = config.getBoolean("detection.rotation-consistency.enabled", true);
        this.rotationConsistencySampleSize = config.getInt("detection.rotation-consistency.sample-size", 10);
        this.rotationConsistencyMaxStdDev = config.getDouble("detection.rotation-consistency.max-std-dev-threshold", 0.01);
        this.rotationConsistencyMinMeanDelta = config.getDouble("detection.rotation-consistency.min-mean-delta", 0.1);
        this.rotationConsistencyThreshold = config.getInt("detection.rotation-consistency.violation-threshold", 3);

        this.centerMassLockEnabled = config.getBoolean("detection.center-mass-lock.enabled", true);
        this.centerMassLockMaxAngle = config.getDouble("detection.center-mass-lock.max-angle-deviation", 0.005);
        this.centerMassLockConsecutiveHits = config.getInt("detection.center-mass-lock.consecutive-hits-threshold", 5);
        this.centerMassLockThreshold = config.getInt("detection.center-mass-lock.violation-threshold", 2);

        this.snapToTargetEnabled = config.getBoolean("detection.snap-to-target.enabled", true);
        this.snapToTargetWatchTicks = config.getInt("detection.snap-to-target.watch-ticks", 3);
        this.snapToTargetMinSnapAngle = config.getDouble("detection.snap-to-target.min-snap-angle", 40.0);
        this.snapToTargetMaxLockAngle = config.getDouble("detection.snap-to-target.max-lock-angle", 2.0);
        this.snapToTargetMaxDistance = config.getDouble("detection.snap-to-target.max-target-distance", 50.0);
        this.snapToTargetThreshold = config.getInt("detection.snap-to-target.violation-threshold", 2);

        this.noLookHitEnabled = config.getBoolean("detection.no-look-hit.enabled", true);
        this.noLookHitMaxAngle = config.getDouble("detection.no-look-hit.max-allowed-angle", 60.0);
        this.noLookHitThreshold = config.getInt("detection.no-look-hit.violation-threshold", 2);

        this.kickEnabled = config.getBoolean("actions.kick.enabled", true);
        this.kickReason = config.getString("actions.kick.reason", "&8[&cDoneAntiTarget&8] &cIllegal elytra behavior detected (%check% / %info%).");

        this.alertEnabled = config.getBoolean("actions.alert.enabled", true);
        this.alertMessage = config.getString("actions.alert.message", "&8[&cDoneAntiTarget&8] &e%player% &cfailed %check% &7(Info: %info%, VL: %vl%/%threshold%)");

        this.consoleLogEnabled = config.getBoolean("actions.console-log.enabled", true);
        this.consoleLogMessage = config.getString("actions.console-log.message", "[DoneAntiTarget] %player% failed %check% (Info: %info%, VL: %vl%/%threshold%)");
    }

    public long getDecayTimeMs() {
        return decayTimeMs;
    }

    public boolean isCheckOnlyGliding() {
        return checkOnlyGliding;
    }

    public boolean isAutoFireworkEnabled() {
        return autoFireworkEnabled;
    }

    public long getAutoFireworkMaxMs() {
        return autoFireworkMaxMs;
    }

    public int getAutoFireworkThreshold() {
        return autoFireworkThreshold;
    }

    public boolean isRotationConsistencyEnabled() {
        return rotationConsistencyEnabled;
    }

    public int getRotationConsistencySampleSize() {
        return rotationConsistencySampleSize;
    }

    public double getRotationConsistencyMaxStdDev() {
        return rotationConsistencyMaxStdDev;
    }

    public double getRotationConsistencyMinMeanDelta() {
        return rotationConsistencyMinMeanDelta;
    }

    public int getRotationConsistencyThreshold() {
        return rotationConsistencyThreshold;
    }

    public boolean isCenterMassLockEnabled() {
        return centerMassLockEnabled;
    }

    public double getCenterMassLockMaxAngle() {
        return centerMassLockMaxAngle;
    }

    public int getCenterMassLockConsecutiveHits() {
        return centerMassLockConsecutiveHits;
    }

    public int getCenterMassLockThreshold() {
        return centerMassLockThreshold;
    }

    public boolean isSnapToTargetEnabled() {
        return snapToTargetEnabled;
    }

    public int getSnapToTargetWatchTicks() {
        return snapToTargetWatchTicks;
    }

    public double getSnapToTargetMinSnapAngle() {
        return snapToTargetMinSnapAngle;
    }

    public double getSnapToTargetMaxLockAngle() {
        return snapToTargetMaxLockAngle;
    }

    public double getSnapToTargetMaxDistance() {
        return snapToTargetMaxDistance;
    }

    public int getSnapToTargetThreshold() {
        return snapToTargetThreshold;
    }

    public boolean isNoLookHitEnabled() {
        return noLookHitEnabled;
    }

    public double getNoLookHitMaxAngle() {
        return noLookHitMaxAngle;
    }

    public int getNoLookHitThreshold() {
        return noLookHitThreshold;
    }

    public boolean isKickEnabled() {
        return kickEnabled;
    }

    public String getKickReason() {
        return kickReason;
    }

    public boolean isAlertEnabled() {
        return alertEnabled;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public boolean isConsoleLogEnabled() {
        return consoleLogEnabled;
    }

    public String getConsoleLogMessage() {
        return consoleLogMessage;
    }
}
