# 🛡️ DoneAntiTarget

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.19%20%2B-blueviolet?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Paper%20%2F%20Spigot-orange?style=for-the-badge)

**DoneAntiTarget** is a highly optimized, specialized anti-cheat solution designed specifically to counter **Elytra Target (Silent Rotations/Aimbot)** and **Auto Firework** exploits in Minecraft. 

Instead of heavy checks that drain your server's performance, DoneAntiTarget focuses deeply on flight-combat vulnerabilities, ensuring precise detection with minimal overhead.

---

## 🚀 Advanced Detection Modules

The plugin analyzes player movement and rotation packets dynamically while gliding to catch even the most advanced client bypasses:

*   **🎆 Auto Firework Module:** Detects macro-like fast slot switching and frame-perfect firework usage (MS threshold based).
*   **🎯 Rotation Consistency:** Catches the perfect, robotic angular consistency common in "Silent Rotation" client modules.
*   **🧲 Center Mass Lock:** Inspects combat hits for magnet-like tracking that stays locked perfectly onto the opponent's hitbox center.
*   **⚡ Snap-to-Target:** Catches sudden, unnatural camera snaps (angle jumps) right at the moment of firework propulsion or target lock.
*   **👁️ No-Look Hit:** Detects combat packets sent at impossible angles, preventing players from hitting targets they aren't even looking at.

---

## ⚙️ Features

*   **⚡ Lightweight & Performance Friendly:** Includes a `check-only-gliding` feature to ensure checks are only calculated when a player is actually flying.
*   **🧹 Automatic Decay:** Infraction violation points automatically decay over a configurable cooldown time.
*   **🛠️ Highly Customizable Actions:** Fully customize how the server reacts to cheaters—supports in-game **Alerts**, automatic **Kicking**, and detailed **Console Logging**.

---

## 🛠️ Commands & Permissions

| Command | Aliases | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/doneantitarget reload` | `/doneat`, `/atarget` | Reloads the configuration file. | `doneantitarget.admin` |

### 🔑 Permissions List:
*   `doneantitarget.admin` - Allows administrators to reload the plugin config (Default: OP).
*   `doneantitarget.alerts` - Allows staff members to receive real-time notification alerts when a player triggers a check (Default: OP).

---
### Youtube Video:
<iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/0AQy144cVYk" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>

## 📦 Default Configuration (`config.yml`)

```yaml
# DoneAntiTarget Configuration

detection:
  decay-time-seconds: 30
  check-only-gliding: true

  auto-firework:
    enabled: true
    max-ms-threshold: 80
    violation-threshold: 5

  rotation-consistency:
    enabled: true
    sample-size: 10
    max-std-dev-threshold: 0.005
    min-mean-delta: 0.1
    violation-threshold: 3

  center-mass-lock:
    enabled: true
    max-angle-deviation: 0.005
    consecutive-hits-threshold: 5
    violation-threshold: 2

  snap-to-target:
    enabled: true
    watch-ticks: 2
    min-snap-angle: 30.0
    max-lock-angle: 1.5
    max-target-distance: 50.0
    violation-threshold: 2

  no-look-hit:
    enabled: true
    max-allowed-angle: 75.0
    violation-threshold: 7

actions:
  kick:
    enabled: true
    reason: "&8[&cDoneAntiTarget&8] &cIllegal elytra behavior detected (%check% / %info%)."
  alert:
    enabled: true
    message: "&8[&cDoneAntiTarget&8] &e%player% &cfailed %check% &7(Info: %info%, VL: %vl%/%threshold%)"
  console-log:
    enabled: true
    message: "[DoneAntiTarget] %player% failed %check% (Info: %info%, VL: %vl%/%threshold%)"
