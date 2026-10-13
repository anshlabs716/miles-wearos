# Development

This page describes how to set up a development environment and work on
MILES Wear OS.

## Requirements

- Android Studio (latest stable)
- JDK 17
- Wear OS 3+ emulator or device

## Getting the code

```bash
git clone https://github.com/anshlabs716/miles-wearos.git
cd miles-wearos
```

## Build

```bash
./gradlew assembleDebug
```

## Run

```bash
./gradlew installDebug
```

## Test

```bash
./gradlew test
```

## Workflow

- Branch from `main` (see [CONTRIBUTING.md](../CONTRIBUTING.md)).
- Keep changes focused and documented.
- Update `CHANGELOG.md` for user-facing changes.
