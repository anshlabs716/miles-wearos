# ⌚ MILES Wear OS

### A standalone, sensor-accurate, privacy-focused activity tracker for Wear OS.

<p align="center">
  <img src="https://img.shields.io/badge/STATUS-1.0.0-success?style=for-the-badge" alt="Version 1.0.0">
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

> **Status:** Version 1.0.0

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

## 🛠️ Technology

- Kotlin
- Jetpack Compose
- Wear Compose Material
- Android SDK
- Sensor & Location APIs
- Room
- OSMdroid / OpenStreetMap
- OSRM + Nominatim
- Wearable Data Layer
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
| Wear app | **1.0.0 released** |
| Step tracking | Implemented |
| GPS tracking + navigation | Implemented |
| Saved routes + route builder | Implemented |
| Progressive training plans | Implemented |
| Fitness pet | Implemented |
| Goals & badges | Implemented |
| Records & PRs | Implemented |
| Compass | Implemented |
| Mirrored workouts | Implemented |
| Move reminders | Next wave |
| Voice navigation (TTS) | Next wave |
| Export / backup (JSON, CSV, GPX) | Next wave |

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