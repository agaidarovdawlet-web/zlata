# zlata

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?logo=android&logoColor=white)
![Build](https://img.shields.io/badge/Build-GitHub%20Actions-informational)
![Release](https://img.shields.io/badge/Release-v1.0.0-blue)
![License](https://img.shields.io/badge/License-MIT-green)

Offline Android demo app for utility-service employees: customer accounts, meters, readings, payments, and local database.

## Who This Project Is For

- recruiters evaluating offline-first Android architecture;
- utility or service-domain workflow prototypes;
- teams that need local mobile tools for internal staff.

## Key Features

- customer and personal account search;
- meter list and verification tracking;
- meter reading entry flow;
- payments view;
- local database copied from app assets for offline usage.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- SQLite asset storage
- local repository layer

## Architecture

```text
UI -> ViewModel -> Repository -> local SQLite database
```

## Screenshots

Screenshot folder:

- [docs/screenshots](docs/screenshots/README.md)

Suggested captures:

- dashboard
- personal accounts
- meters
- reading form
- payments

## GIF Or Video Demo

- APK is distributed via GitHub Releases

## Installation And Run

```bash
git clone https://github.com/agaidarovdawlet-web/zlata.git
cd zlata
./gradlew assembleDebug
```

APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Local Database

Main data file:

- `app/src/main/assets/energosbyt_plus.db`

## Project Structure

```text
app/src/main/java/com/example/zlata/
├── data/
├── ui/
└── viewmodel/
```

## What I Implemented Personally

- offline-first staff workflow concept;
- local database integration from bundled assets;
- search and list screens for accounts and meters;
- reading and payment-related business flow UI;
- APK release workflow.

## Status

Portfolio/demo business app focused on offline workflows and local database usage.

## Plans

- add real screenshots;
- refine repository and DB docs;
- extend analytics and validation around readings and account state.
