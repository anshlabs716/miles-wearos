# ⌚ MILES Wear OS

### The standalone GPS + heart-rate tracker for your wrist.

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

**MILES Wear OS** is the official wearable companion to the MILES activity ecosystem. It puts real sensor data — GPS, heart rate, barometer altitude, steps, cadence — straight on the dial. Standalone or mirrored, online or offline, it **never invents a number**.

> **Status:** Version 1.0.0 — watchs-first, works on phones too

## ✨ Features

- Real step counting with midnight rollover baselines (today = today, not since-reboot)
- Real GPS tracking + turn-by-turn navigation (OSRM) + offline tile layers
- Saved routes — keep a live route or build one from pins, then follow offline
- Walking, running, cycling, hiking, and indoor workout recording
- Progressive training plans — C25K, 5K Improver, 10K Builder, HIIT
- Local workout history, records, and PRs from real sessions
- Fitness Pet 🐦, weekly goals, and honest badge progress
- Mirrored workout HUD from the phone app
- Compass, water lock, and sensor diagnostics
- Wearable Data Layer sync with the MILES phone app
- Pure OLED black design system with high-contrast accents

MILES does not intentionally generate fake GPS, heart-rate, or step data.

## 🔒 Privacy

MILES Wear OS is designed to work without requiring:

- A MILES account
- A MILES cloud service
- A subscription
- Advertising
- Forced analytics
- A forced Google account

Your activity data stays on your wrist until you choose to sync it.

## 🛠️ Technology

- Kotlin
- Jetpack Compose + Wear Compose Material
- Android SDK
- Sensor & Location APIs (heart rate, step counter, GPS, barometer)
- Room (local database)
- OSMdroid + OpenStreetMap tiles
- OSRM routing & Nominatim search
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

- Export / backup (JSON, CSV, GPX)
- Move reminders
- Voice navigation
- More sensor support
- Better GPS filtering and route smoothing
- Continued MILES Studio development

## 📄 License

**GPLv3**

---

**Real data. Real tracking. Your wrist. Your data.**

Made by **AnshLabs716**.

**Android + Linux only.**