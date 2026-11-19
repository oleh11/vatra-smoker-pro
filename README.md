# Vatra Smoker Pro — Pellet Grill Companion

<p align="center">
  <a href="https://github.com/oleh11/vatra-smoker-pro/releases/latest"><img src="https://img.shields.io/github/v/release/oleh11/vatra-smoker-pro?style=for-the-badge&color=111111&logo=android&logoColor=black&label=Release" alt="Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-white?style=for-the-badge" alt="License" /></a>
  <img src="https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" />
</p>

> A free, open-source Android app for **Camp Chef-based Wi-Fi pellet grills**, including the Cabela's Pro Series 36". Direct Bluetooth, no account, no cloud, **no ads, no tracking, no internet permission**.
>
> Independent project, not affiliated with Cabela's or Camp Chef.

## 📥 Download & Install

| Where | How |
| :--- | :--- |
| **Direct APK** | [**Download the latest APK**](https://github.com/oleh11/vatra-smoker-pro/releases/latest) |
| **Obtainium** (auto-updates from GitHub) | [**Add to Obtainium**](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/oleh11/vatra-smoker-pro) |
| **Google Play** | Internal testing now, public release after hardware testing |

> **Sideloading:** allow *"Install from this source"* when Android asks. Close the official Cabela's app first: the grill accepts one Bluetooth phone at a time.

## ✨ Features

- **Control:** start up, shut down, set target temperature and smoke level (changes are sent only after you press Confirm).
- **Monitor:** chamber temperature, four meat probes (optional), faults and a live cook chart.
- **Alarms:** foreground service with notification and vibration when a probe hits its target, screen off or on.
- **Reliable connection:** 15 s timeout, automatic retry on Bluetooth error 133, auto-reconnect, and in-app pairing when the grill requires it.
- **Wi-Fi setup:** send your home network to the grill over Bluetooth.
- **Cook planner:** brisket, pork shoulder, 3-2-1 ribs, salmon, turkey and reverse-sear presets.
- **Simulator:** try everything without a grill.
- **Diagnostics:** live GATT log and raw notifications for troubleshooting.

Privacy details: [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

---

## 📱 Project Architecture

```
com.oleh11.vatrasmoker/
├── SmokerApp.kt                 # Application class & service initializer
├── MainActivity.kt              # Edge-to-edge Compose host & permissions
├── data/
│   ├── ble/                     # Modern BLE manager, GATT UUIDs & packet codec
│   ├── model/                   # Domain models (SmokerState, MeatProbe, Presets)
│   ├── repository/              # Reactive repository layer with StateFlow
│   ├── simulator/               # Realistic thermal & physics simulator engine
│   └── wifi/                    # Wi-Fi network scanner & provisioning
├── service/
│   └── CookMonitoringService.kt # Foreground service with persistent notification & alarms
└── ui/
    ├── components/              # Radial temp gauge, probe cards, smoke dial, live charts
    ├── navigation/              # Compose navigation suite & destinations
    ├── screens/                 # Dashboard, Scanner, BBQ Planner, Wi-Fi, Settings
    └── theme/                   # Material 3 BBQ Ember & Charcoal theme
```

---

## 🛠️ Connection troubleshooting

The app now handles the common Android BLE failures on its own:

- **Error 133 / timeouts**: the link is closed cleanly and retried up to 4 times with back-off; a 15 s connect timeout replaces the old "stuck on Connecting".
- **Dropped link**: reconnects automatically unless you tapped Disconnect.
- **Pairing (status 5/15)**: if the grill demands encryption, the app starts Android pairing, waits for it, and retries the failed step.
- **Only one phone at a time**: the grill allows a single BLE client. Close the official Cabela's app first.
- Open **Diagnostics** to see the live GATT log and raw notifications if something still misbehaves.
