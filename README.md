# ⌚ MILES Wear OS

### A standalone, sensor-accurate, privacy-focused activity tracker for Wear OS.

<p align="center">
  <img src="https://img.shields.io/badge/STATUS-1.0.4-success?style=for-the-badge" alt="Version 1.0.4">
  <img src="https://img.shields.io/badge/DEVELOPMENT-ACTIVE-yellow?style=for-the-badge" alt="Active Development">
  <img src="https://img.shields.io/badge/PLATFORM-WEAR%20OS%203%2B-4285F4?style=for-the-badge&logo=wearos&logoColor=white" alt="Wear OS">
  <img src="https://img.shields.io/badge/KOTLIN-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/JETPACK%20COMPOSE-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/MAPS-OPENSTREETMAP-7EBC6F?style=for-the-badge&logo=openstreetmap&logoColor=white" alt="OpenStreetMap">
  <img src="https://img.shields.io/badge/NAVIGATION-OSRM%20OFFLINE-00E676?style=for-the-badge" alt="OSRM Navigation">
  <img src="https://img.shields.io/badge/SENSORS-REAL%20DATA-FF5722?style=for-the-badge" alt="Real Sensors">
  <img src="https://img.shields.io/badge/PRIVACY-LOCAL--FIRST-8A2BE2?style=for-the-badge" alt="Privacy">
  <img src="https://img.shields.io/badge/LICENSE-GPLv3-blue?style=for-the-badge" alt="GPLv3 License">
</p>

> 📱 **MILES Android is the companion project:** https://github.com/anshlabs716/miles

**MILES Wear OS** is a modern Wear OS activity and step tracker focused on real sensor data, GPS tracking, saved routes, training plans, and a clean OLED-first UI.

> **Status:** Version 1.0.4

## 🤔 Why not just use the watch's built-in step tracker?

Your watch already counts steps. Google, Samsung, Apple, Garmin, Fitbit and every
other vendor ship a step tracker, and they all have one thing in common: **it
phones home.**

The built-in tracker is not a local tool. It is a front end for a cloud service
that your watch vendor runs on your behalf:

- **Your watch manufacturer collects your data.** Samsung collects through
  Samsung Health, Apple through Apple Health + iCloud, Google through Fit. Steps,
  heart rate, workouts, sleep, GPS routes — synced to their servers, tied to an
  account, used for their product analytics and advertising businesses.
- **Google collects your data** through Fit and, if you use Wear OS, through the
  Play Services layer that any app on the watch can reach.
- **You have no say in it.** There is no switch for "keep my health data on the
  device." Even accounts with sync disabled still send telemetry. Deleting the
  manufacturer's app breaks your watch's ecosystem; you cannot replace it with
  your own.
- **It is not yours.** The data sits in someone else's database, subject to
  their retention policy, their breach history, and their terms — not yours.

**MILES Wear OS collects nothing.** There is no account, no server, no analytics,
no crash reporting, no telemetry, and no Google Play Services. Everything —
every workout, every route, every personal best — is stored in a local Room
database on your watch and nowhere else. Uninstall it and it is gone, with no
copy left behind on anyone's servers.

Concretely, on a network capture with no opt-in:

| | Built-in tracker | MILES Wear OS |
| --- | --- | --- |
| Data leaves the watch | Yes, automatically | **Only what you explicitly trigger** |
| Needs an account | Yes | No |
| Requires Google Play Services | Usually | **Never** |
| Works with GMS removed | No | Yes |
| Data readable/deletable by you | Through the vendor only | Directly, on-device |
| Open source | No | **Yes, GPL-3.0** |

You can check that claim yourself: MILES has no proprietary dependencies, so the
APK contains no trackers to find. Install it, put the watch in airplane mode
after syncing what you want, and it keeps working.

The built-in tracker is fine if you are happy handing your health data to a
company. If you are not, this is the alternative — a real tracker that measures
the same hardware and tells nobody.

## ✨ Features

- Real step counting
- Real GPS tracking and turn-by-turn navigation
- Walking, running, cycling, hiking, and workout recording
- Saved routes and route builder
- Progressive training plans
- Local activity history, records, and PRs
- OpenStreetMap-based mapping
- Fitness Pet 🐦
- Weekly goals and badges
- Mirrored workout HUD
- Compass and water lock
- Sensor diagnostics
- Export & backup (GPX, CSV, JSON) with on-device restore
- Import workouts + backups from any app (GPX, JSON, share-to-MILES)
- Street + satellite map on every workout (OSM & Esri tiles)
- Detects the MILES phone app: on this device (package check) or nearby (Wearable capability) with tap-to-open
- Phone sync with **zero Google Play Services**: local-network discovery (UDP beacon) + TCP messaging for live metrics, remote workout control, HR/cadence streaming and offline queue sync
- Move reminders (sitting-streak nags, skip during workouts)
- Voice navigation (spoken turn-by-turn cues)
- Automatic km/mi splits with real pace
- Resting heart rate (real lowest idle HR per day)
- Daily calorie goal with dashboard progress
- BLE heart-rate straps and cadence sensors
- Privacy-focused, local-first design

MILES does not intentionally generate fake GPS, heart-rate, or step data.

## 🔒 Privacy

MILES Wear OS is designed to work without requiring:

- A MILES account
- A MILES cloud service
- A subscription
- Advertising
- Forced analytics
- A forced Google account

Your activity data stays under your control.

### Network use

MILES Wear OS stores everything locally, but a few features contact public
services. None require an account, and none send activity history, heart rate,
or workout data anywhere:

| Feature | Destination | What is sent |
| ------- | ----------- | ------------ |
| Weather | `api.open-meteo.com` | Device latitude/longitude, used to fetch current conditions |
| Place search | `nominatim.openstreetmap.org` | Your typed search query and map area |
| Turn-by-turn routing | `router.project-osrm.org` | Start and destination coordinates |
| Map tiles | OpenStreetMap, CARTO, CyclOSM, OpenTopoMap, USGS World Imagery | Tile coordinates (the tile you are viewing) |

Satellite imagery comes from **USGS World Imagery** (via Esri's tile service);
street and terrain tiles are OpenStreetMap and friends. Attribution is shown on
the map.

### Phone sync is opt-in

**Phone Sync is off by default.** With it disabled the app does not broadcast
any discovery beacon, does not open a listening TCP port, and does no local
network activity at all. Turn it on in Settings to pair with the phone app over
your own Wi-Fi.

Discovery uses a **random identifier generated per install**, not the device's
`ANDROID_ID`. No hardware identifier, serial number, or advertising ID is ever
transmitted.

## 🛠️ Technology

- Kotlin
- Jetpack Compose
- Wear Compose Material
- Android SDK
- Sensor & Location APIs
- Room
- OSMdroid / OpenStreetMap
- OSRM (turn-by-turn routing) + Nominatim (place search)
- Local-network sockets (UDP discovery + TCP) for phone sync
- Coroutines

## 📦 Build

Requirements: **JDK 17, Android SDK, Git**

```bash
git clone https://github.com/anshlabs716/miles-wearos.git
cd miles-wearos
gradle :app:assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 🚧 Development

| Area | Status |
| --- | --- |
| Wear app | **1.0.4 released** |
| Step tracking | Implemented |
| GPS tracking + navigation | Implemented |
| Saved routes + route builder | Implemented |
| Progressive training plans | Implemented |
| Fitness pet | Implemented |
| Goals & badges | Implemented |
| Records & PRs | Implemented |
| Compass | Implemented |
| Mirrored workouts | Implemented |
| Move reminders | Implemented |
| Voice navigation (TTS) | Implemented |
| Export / backup (JSON, CSV, GPX) | Implemented |
| Import from any app (GPX, JSON, backup) | Implemented |
| Satellite + street map tiles | Implemented |
| BLE HR + cadence sensors | Implemented |
| GMS-free phone sync (local network) | Implemented |

## 🗺️ Roadmap

- More sensor support
- Better GPS filtering and route smoothing
- Health Connect integration
- Continued MILES Studio development

## 📄 License

**GPLv3**

---

**Real data. Real tracking. Your wrist. Your data.**

Made by **AnshLabs716**.

**Android + Linux only.**