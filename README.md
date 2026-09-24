<div align="center">

# ⌚ MILES Wear OS

**The standalone GPS + heart-rate tracker for your wrist. Zero fake stats. Pure privacy.**

[![Release](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fapi.github.com%2Frepos%2Fanshlabs716%2Fmiles-wearos%2Freleases%2Flatest&query=tag_name&label=latest%20release&color=00E676)](https://github.com/anshlabs716/miles-wearos/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Wear%20OS%203%2B-brightgreen.svg)](https://developer.android.com/wear)
[![Language](https://img.shields.io/badge/Language-Kotlin%20100%25-purple.svg)](https://kotlinlang.org)
[![minSdk](https://img.shields.io/badge/min%20SDK-30-FF5722)](https://developer.android.com/tools/releases/platforms)
[![targetSdk](https://img.shields.io/badge/target%20SDK-35-00B0FF)](https://developer.android.com/tools/releases/platforms)
[![Build](https://img.shields.io/badge/Build-assembleDebug%20%2B%20tests%20%E2%9C%94-brightgreen)](https://github.com/anshlabs716/miles-wearos/actions)

[![GitHub stars](https://img.shields.io/github/stars/anshlabs716/miles-wearos?style=social)](https://github.com/anshlabs716/miles-wearos)
[![GitHub forks](https://img.shields.io/github/forks/anshlabs716/miles-wearos?style=social)](https://github.com/anshlabs716/miles-wearos/fork)

</div>

**MILES Wear OS** is the official standalone **and** companion app for the MILES activity ecosystem. It puts real-time wrist telemetry — GPS, heart rate, barometer altitude, steps, cadence — straight on the dial, works fully offline, and **never invents a number**. Every stat you see comes from real sensors, real GPS, or real workout history.

---

## ✨ Feature Highlights

| Feature | Why it's cool |
| :--- | :--- |
| 📍 **Saved Routes** | Keep any live GPS route, or build one from pins, then *follow it anytime* — real progress + ETA + arrival haptics, offline. |
| 🏋️ **Progressive Training Plans** | C25K, 5K Improver, 10K Builder & HIIT — every day is a real interval/goal workout; finishing it checks the day off. |
| 🗺️ **Live Map + Navigation** | OSRM turn-by-turn, offline tile layers, saved pins, search, and velcro-fast route drawing. |
| ❤️ **Real Heart Rate HUD** | Instant sensor feedback with hardware accuracy level (0–4). No sensor? It says so. No fabrications, ever. |
| 🐾 **Fitness Pet** | Adopt Pixel, earn treats with *real* steps, watch them grow. |
| 🏅 **Weekly Goals + Badges** | Real-distance streaks and honest locked-progress badges. |
| 📊 **Records & PRs** | Fastest km, longest run, most calories — pulled from real workout history. |
| 🎙️ **Voice Navigation** | Spoken turn-by-turn cues. *(coming in the next wave)* |
| 💾 **Export / Backup** | JSON, CSV & GPX export of real data. *(coming in the next wave)* |

---

## 🌟 Core Features

### 1. Standalone & Companion Dual Operation
- **Standalone Mode** — leave the phone behind. Track runs, walks, hikes, rides with on-wrist GPS, heart rate, barometer altitude and steps. Everything is saved to the local Room database and queued for sync when you reconnect.
- **Companion Mode** — running with the phone, the watch becomes a glanceable HUD, streaming live heart rate and cadence back to the MILES mobile app over the Wearable Data Layer.

### 2. Supported Activities
- 🚶 **Walking** — GPS distance, heart rate, steps, active calories.
- 🏃 **Running** — real-time pace, cadence (SPM), kilometer split vibration cues, HR zone alerts.
- 🚴 **Cycling** — GPS speed (km/h or mph), barometer elevation gain, heart rate.
- ⛰️ **Hiking** — barometric altitude profile, distance, HR zones.
- ⚡ **Other / Indoor** — sensor-driven HR + calories, GPS off to save battery.

### 3. Zero-Fake-Stats Sensor Pipeline
- **Real Heart Rate** — instantaneous, with hardware accuracy level. When the sensor is unavailable it *reports unavailable*.
- **Steps** — hardware step counter with **midnight rollover handling**, so "today's steps" is actually today's steps (daily baseline reset — no reboot inflation).
- **GPS State Awareness** — explicit `GPS Searching…` ➔ `GPS Ready` ➔ `GPS Unavailable / Indoor Mode` transitions.
- **Eco Mode** — sampling automatically drops to battery-saver mode below 20% battery.

### 4. Pure OLED Design System
- Pure black `#000000` canvas to save OLED power and prevent burn-in.
- High-contrast accents: Neon Cyan `#00B0FF`, Vivid Green `#00E676`, Coral Flame `#FF5722`, Electric Amber `#FFD600`.
- Full rotary crown/bezel support with snap-friendly scrolling.
- One-tap **Water Lock** for sweaty, rainy workouts.

---

## 🗺️ Routes & 🏋️ Training Plans

### Saved Routes (real GPS, offline follow)
1. Navigate somewhere on the map → tap **Save** (next to Stop) → the live OSRM route is stored.
2. — or — drop 2+ pins → tap **🧩 Route** → connect & **Save**.
3. Open the **📍 Routes** dashboard chip, pick any route, hit **Follow ➜** — it redraws and tracks your real position, remaining distance, ETA, with arrival haptics. No network needed.

### Progressive Training Plans
- **Couch to 5K** (8 weeks) · **5K Improver** (6) · **10K Builder** (8) · **HIIT Blaster** (4
- 3 real sessions per week with rest days — e.g. `6 × 3 min / 2:30 rest`.
- Every day launches the live tracking service with real sensors; **finishing the workout checks the day ✓**. Progress bars show honest `N/24` — never fake completion.

---

## 🔒 Privacy & Local-First Architecture

- **100% local storage** — embedded SQLite/Room database, no cloud, no account, no servers.
- **Zero third-party trackers** — no ads, no analytics, no tracking SDKs.
- **Safe sync only** — data leaves the wrist solely over the local Wearable Data Layer to your paired MILES phone app.

---

## 📱 Navigation Architecture

```text
Home (Dashboard)
 ├── Start Workout ── Walking / Running / Cycling / Hiking / Other
 ├── Live Workout HUD ── Metrics · HR Zone & Cadence · Controls & Water Lock
 ├── Workout Summary ── Save / Discard (real Room persistence)
 ├── History ── Workout Details & sync state
 ├── 📍 Routes ── saved routes · follow · delete
 ├── 🏋️ Training Plans ── 4 programs · week/day view · start day
 ├── 🐾 Pet · 🏅 Goals & Badges · 📊 Records
 ├── 🗺️ Maps ── live nav · pins · route builder · layers
 ├── 🧭 Compass
 ├── Settings ── units · display · sensors · privacy
 └── Sensor Diagnostics ── hardware monitor & queue flush
```

---

## 📡 Cross-Device Wearable Protocol

| Channel | Direction | Purpose | Payload |
| :--- | :--- | :--- | :--- |
| `/miles/sensors/hr` | Watch ➔ Phone | Live heart rate stream | `{"bpm": 142, "accuracy": 3, "timestamp": ...}` |
| `/miles/sensors/cadence` | Watch ➔ Phone | Live cadence | `{"cadence": 168, "timestamp": ...}` |
| `/miles/workout/control` | Bi-directional | Start / Pause / Resume / Finish | `{"action": "PAUSE", "workoutType": "RUN"}` |
| `/miles/metrics/live` | Phone ➔ Watch | Mirrored metrics | `{"sec": 340, "m": 1020.0, "cal": 85, "hr": 142}` |
| `/miles/queue/flush` | Watch ➔ Phone | Offline batch sync | `{"items": [...], "count": 12}` |

---

## 🛠️ Build & Development

### Prerequisites
- Android Studio Ladybug or newer
- Android SDK 34/35
- Wear OS 3+ emulator or physical device

### Build Commands
```bash
# Debug APK
gradle :app:assembleDebug

# Unit tests
gradle :app:testDebugUnitTest
```

### APK Output
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔗 Ecosystem

| Project | Repository | Role |
| :--- | :--- | :--- |
| **MILES Android** | [anshlabs716/miles](https://github.com/anshlabs716/miles) | Phone app: dashboard, route mapping, long-term history |
| **MILES Wear OS** | [anshlabs716/miles-wearos](https://github.com/anshlabs716/miles-wearos) | This repo — the watch app |

---

## 📄 License

**GNU General Public License v3.0 (GPLv3)** — see the [LICENSE](LICENSE) file.

<div align="center">

*Built one real sensor reading at a time.* 🚀

</div>