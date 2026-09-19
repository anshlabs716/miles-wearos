# ⌚ MILES Wear OS

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Wear%20OS%203%2B-brightgreen.svg)](https://developer.android.com/wear)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%20100%25-purple.svg)](https://kotlinlang.org)

**MILES Wear OS** is the official standalone and companion Wear OS application for the **MILES** activity tracking ecosystem. Engineered from the ground up for Wear OS 3+ (API 30+ targeting API 34/35), it delivers real-time wrist telemetry, offline-first reliability, battery-efficient sensor sampling, and zero-fake stats with pure privacy.

---

## 🔗 Ecosystem Repositories

| Project | Repository | Role |
| :--- | :--- | :--- |
| **MILES Android** | [anshlabs716/miles](https://github.com/anshlabs716/miles) | Primary Android Mobile App (Dashboard, Route Mapping, Long-term History) |
| **MILES Wear OS** | [anshlabs716/miles-wearos](https://github.com/anshlabs716/miles-wearos) | Dedicated Watch App (Real-time wrist HUD, Live Sensors, Standalone Tracking) |

---

## 🌟 Core Features

### 1. Standalone & Companion Dual Operation
- **Standalone Mode**: Leave your phone behind. Track outdoor runs, walks, hikes, and rides with on-wrist GPS, heart rate, barometer altitude, and step tracking. All data is saved safely in a local Room database and queued for automatic synchronization whenever reconnecting with your phone.
- **Companion Mode**: When running with your phone, the watch acts as a high-visibility, glanceable HUD mirroring real-time metrics and streaming continuous hardware heart rate and cadence back to the MILES mobile app.

### 2. Supported Activities
- 🚶 **Walking** — GPS distance, wrist heart rate, step count, and active calories.
- 🏃 **Running** — Real-time pace, cadence (SPM), kilometer split vibration cues, and HR zone alerts.
- 🚴 **Cycling** — GPS ground speed (km/h or mph), elevation gain via barometer, and heart rate.
- ⛰️ **Hiking** — Barometric altimeter elevation profile, distance, and heart rate zones.
- ⚡ **Other / Indoor** — Sensor-driven heart rate and calorie tracking with GPS disabled for maximum battery preservation.

### 3. Pure OLED Design System
- **Pure Black (#000000)** canvas across all screens to minimize OLED pixel power draw and prevent screen burn-in.
- **High-Contrast Accents**: Neon Cyan (`#00B0FF`), Vivid Green (`#00E676`), Coral Flame (`#FF5722`), and Electric Amber (`#FFD600`).
- **Ergonomic Rotary Input**: Full support for physical crown and bezel rotation with snap-friendly scrolling on circular displays.
- **Water Lock Mode**: Dedicated one-tap water lock rejecting accidental sweat and raindrop touch inputs during vigorous workouts.

### 4. Zero-Fake-Stats Sensor Pipeline
- **Real Heart Rate**: Instantaneous sensor feedback with hardware accuracy levels (0–4). Clearly reports when the sensor is unavailable without fabricating heart rates.
- **Steps**: Hardware step counter & detector with midnight counter rollover handling. Accurately distinguishes daily steps from workout steps.
- **GPS State Awareness**: Explicit status transitions (`GPS Searching…` ➔ `GPS Ready` ➔ `GPS Unavailable` / `Indoor Mode`) so you always know when your route fix is locked.
- **Power Optimization**: Automatically switches sensor sampling frequency to battery-saving eco mode when battery level drops below 20%.

---

## 🔒 Privacy & Local-First Architecture

MILES Wear OS is built around a strict privacy-first foundation:

- **100% Local Storage**: All workouts and metrics are stored locally in an embedded SQLite/Room database (`miles_wear.db`).
- **No Cloud Dependency**: No MILES account, no remote cloud database, and no mandatory servers.
- **Zero Third-Party Trackers**: No ads, no tracking SDKs, and no telemetry analytics.
- **Safe Synchronization**: Data only leaves your wrist over the local Google Play Services Wearable Data Layer (`MessageClient`, `CapabilityClient`) to your paired MILES phone app.

---

## 📱 Navigation Architecture

```text
Home (Dashboard)
 ├── Start Workout
 │    ├── Walking
 │    ├── Running
 │    ├── Cycling
 │    ├── Hiking
 │    └── Other
 │
 ├── Live Workout HUD
 │    ├── Page 1: Main HUD Metrics & GPS Status
 │    ├── Page 2: Heart Rate Zone Ring & Cadence
 │    └── Page 3: Workout Controls & Water Lock
 │
 ├── Workout Summary (Complete Recap)
 │    ├── Save Workout (Persists to Room & triggers sync)
 │    └── Discard Workout (Confirmation dialog)
 │
 ├── History
 │    └── Workout Details (Full breakdown & local sync state)
 │
 ├── Settings
 │    ├── Distance Units (Metric km vs Imperial mi)
 │    ├── Display (OLED Black, Keep Screen On)
 │    ├── Sensors (GPS, Heart Rate, Step Tracking)
 │    └── Privacy Guarantee & About
 │
 └── Sensor Diagnostics (Hardware Monitor & Queue Flush)
```

---

## 📡 Cross-Device Wearable Protocol

Communication with the MILES phone application occurs via the Wearable Data Layer API:

| Channel / Path | Direction | Purpose | Payload |
| :--- | :--- | :--- | :--- |
| `/miles/sensors/hr` | Watch ➔ Phone | Live Heart Rate Stream | `{"bpm": 142, "accuracy": 3, "timestamp": ...}` |
| `/miles/sensors/cadence` | Watch ➔ Phone | Live Cadence Stream | `{"cadence": 168, "timestamp": ...}` |
| `/miles/workout/control` | Bi-directional | Start / Pause / Resume / Finish | `{"action": "PAUSE", "workoutType": "RUN"}` |
| `/miles/metrics/live` | Phone ➔ Watch | Mirrored Workout Metrics | `{"sec": 340, "m": 1020.0, "cal": 85, "hr": 142}` |
| `/miles/queue/flush` | Watch ➔ Phone | Offline Queue Batch Sync | `{"items": [...], "count": 12}` |

---

## 🛠️ Build & Development

### Prerequisites
- Android Studio Ladybug or newer
- Android SDK 34/35
- Wear OS 3+ emulator or physical Wear OS device

### Build Commands
```bash
# Clean and compile debug APK
gradle :app:assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

---

## 📄 License

MILES Wear OS is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for details.
