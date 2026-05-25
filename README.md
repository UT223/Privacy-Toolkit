# Privacy Toolkit — Android App

An all-in-one **mobile privacy toolkit** built with Kotlin, MVVM, Jetpack, and Room. All analysis runs entirely on-device — no internet permission, no cloud, no data leaks.

---

## Features

| Module | Description |
|--------|-------------|
| **App Permission Analyzer** | Lists all user-installed apps, scores each by the sensitivity of its declared permissions, and highlights high-risk ones |
| **Wi-Fi Security Checker** | Reads the current Wi-Fi connection and classifies it: Open (Danger), WEP (Caution), WPA/WPA2/WPA3 (Safe) |
| **QR Code Scanner** | Scans QR codes with ZXing and checks URLs for HTTPS, shorteners, suspicious TLDs, and phishing keywords — all offline |
| **Privacy Dashboard** | Aggregates findings into a 0–100 privacy score with module quick-stats and navigation |

---

## Architecture

```
com.privacytoolkit
├── data
│   ├── database/          — Room entities (AppScanResult, NetworkHistory, QRScanHistory)
│   │                         + DAOs + PrivacyDatabase singleton
│   └── repository/        — AppRepository, NetworkRepository, QRRepository
│                             (all business logic lives here)
├── viewmodel/             — AppViewModel, NetworkViewModel, QRViewModel
│                             (AndroidViewModel, exposes LiveData to UI)
└── ui/
    ├── MainActivity        — Single-Activity host with NavHostFragment + BottomNav
    ├── dashboard/          — DashboardFragment (aggregated score + cards)
    ├── apps/               — AppsFragment + AppRiskAdapter (RecyclerView)
    ├── wifi/               — WifiFragment + NetworkHistoryAdapter
    ├── qr/                 — QRFragment (ZXing continuous scan) + QRHistoryAdapter
    └── about/              — AboutFragment (privacy-first info)
```

**Pattern:** MVVM — Fragments observe `LiveData` from ViewModels; ViewModels call Repositories; Repositories interact with `PackageManager`/`WifiManager`/Room.

---

## Setup

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Kotlin 1.9+
- A device/emulator running Android 7.0+ (API 24)

### Open the project
1. Clone or unzip this project
2. Open **Android Studio → Open → select the `PrivacyToolkit` folder**
3. Let Gradle sync finish (first sync downloads ~200 MB of dependencies)
4. Run on a physical device or emulator (API 24+)

> **Tip:** Use a physical device for best results — the emulator has no real Wi-Fi or installed apps to scan.

---

## Permissions

| Permission | Used by |
|------------|---------|
| `QUERY_ALL_PACKAGES` | App Permission Analyzer — lists installed packages |
| `ACCESS_WIFI_STATE` | Wi-Fi Checker — reads SSID and capabilities |
| `ACCESS_FINE_LOCATION` | Wi-Fi Checker — required by Android to read Wi-Fi scan results |
| `CAMERA` | QR Scanner — camera preview for scanning |
| `ACCESS_NETWORK_STATE` | Connectivity check |

**`INTERNET` is intentionally absent** — this app cannot make any network requests.

---

## Risk Scoring

### App Permissions
| Score | Label | Criteria |
|-------|-------|----------|
| 3 | High | Any high-risk permission (mic, SMS, call log, contacts, phone state) |
| 2 | Medium | 1–2 medium-risk permissions (camera, location, storage, biometric) |
| 1 | Low | Only low-risk permissions (internet, NFC, vibrate) |
| 0 | Safe | No sensitive permissions found |

### Wi-Fi Networks
| Rating | Label | Security |
|--------|-------|----------|
| 0 | Safe | WPA / WPA2 / WPA3 |
| 1 | Caution | WEP (outdated) or unknown |
| 2 | Danger | Open network (no password) |

### QR Codes
Offline heuristic checks:
- Protocol: HTTP vs HTTPS
- Host type: IP address instead of domain name
- URL shorteners: bit.ly, t.co, tinyurl, etc.
- Suspicious TLDs: .tk, .ml, .xyz, etc.
- Phishing keywords: login, verify, password, credential, invoice, prize, etc.
- URL length > 300 chars
- Nested redirects (double `http` in URL)

---

## Data Storage

All history is stored in a **local Room SQLite database** (`privacy_toolkit_db`).

| Table | Stores |
|-------|--------|
| `app_scan_results` | App name, package, risky permissions, risk score |
| `network_history` | SSID, security type, safety rating, timestamp |
| `qr_scan_history` | Content, type, safety flag, timestamp |

Data never leaves the device.

---

## Key Dependencies

```gradle
// UI
com.google.android.material:material:1.11.0
androidx.navigation:navigation-fragment-ktx:2.7.4

// Architecture
androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2
androidx.lifecycle:lifecycle-livedata-ktx:2.6.2

// Database
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1

// QR Scanning
com.journeyapps:zxing-android-embedded:4.3.0
com.google.zxing:core:3.5.1

// Async
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3
```

---

## Testing Checklist

- [ ] App list loads and shows risky permission chips
- [ ] Re-scan button refreshes results
- [ ] Wi-Fi check shows correct SSID and security type
- [ ] Open hotspot shows "Danger" badge
- [ ] QR scanner opens camera and decodes codes
- [ ] HTTP URL flagged as insecure
- [ ] Bit.ly URL flagged as short-link
- [ ] Safe HTTPS URL shows green check
- [ ] History persists after app restart
- [ ] Dashboard score updates after each module scan
- [ ] Camera permission denied → graceful fallback shown
- [ ] Location permission denied → Wi-Fi check still runs (reduced detail)
- [ ] Back navigation exits correctly from all screens

---

## Extending the App

- **Add more risk signals** — extend `highRiskPermissions` / `mediumRiskPermissions` in `AppRepository`
- **Improve QR analysis** — add more keyword patterns or a local blocklist in `QRRepository`
- **Wi-Fi scan history** — the `NetworkRepository` already saves every check; build a chart in the Wi-Fi fragment
- **Export report** — add a share button that exports the Room data as a CSV using `FileProvider`
- **Dark mode** — the theme extends `DayNight`; add a `values-night/themes.xml` for full dark support

---

## Privacy Statement

> This application performs all analysis locally on your device.
> No data is transmitted to any external server.
> No analytics SDK is included.
> The app does not declare the `INTERNET` permission and is therefore technically incapable of making network requests.
