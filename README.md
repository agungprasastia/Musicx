# Musicx

[![Android CI](https://github.com/Agungprasastia/Musicx/actions/workflows/ci.yml/badge.svg)](https://github.com/Agungprasastia/Musicx/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-green.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](LICENSE)

An open-source, private, and ad-free Android music player with an editorial dark aesthetic. Musicx streams from YouTube Music, scans local audio, synchronizes Spotify saved music, and scrobbles to ListenBrainz.

---

## Features

- **YouTube Music Streaming:** Browse and search music catalog via InnerTube endpoints without ads.
- **Spotify Library Sync & Matching:**
  - Login via in-app browser or paste public playlist links.
  - Sync playlists and liked songs.
  - Intelligent track matching with preview and review decisions.
  - Hierarchical navigation with seamless back navigation.
- **Local Audio Library:** Scan and play tracks stored on your device with instant responsiveness.
- **Editorial Dark UI:** Tactile dark canvas (`#0D0D0F`), deep surfaces (`#18181B`), warm copper accents, and edge-to-edge system bar integration.
- **Background Playback & Lock-Screen Media:** Built on AndroidX Media3 `MediaSessionService` and `ExoPlayer` with rich notifications, transport controls, and lock-screen artwork.
- **Playback Enhancements:** Variable playback speed, skip silence toggle, sleep timer, and system equalizer integration.
- **ListenBrainz Scrobbling:** Automatic tracking with Keystore-encrypted token storage.
- **Privacy First:** No analytics or third-party ad trackers. Auth secrets and session cookies are encrypted using hardware-backed Android Keystore AES-GCM.

---

## Tech Stack & Architecture

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose & Material 3
- **Audio Engine:** AndroidX Media3 (`media3-exoplayer`, `media3-session`, `media3-common`)
- **Networking:** OkHttp & Kotlinx Serialization
- **Image Loading:** Coil Compose
- **Security:** Android Keystore (`Cipher`, `KeyStore`, AES/GCM/NoPadding)
- **Testing:** JUnit 4 unit test suite with reproducible repository fakes and Android Lint integration

---

## Getting Started

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17+
- Android device or emulator running Android 8.0 (API 26) or higher

### Building from Source

```bash
# Clone the repository
git clone https://github.com/agungprasastia/Musicx.git
cd Musicx

# Build debug APK
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```

### Running Tests & Lint

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run Android Lint analysis
./gradlew lintDebug
```

---

## Project Structure

```
Musicx/
├── .github/workflows/         # CI automation (lint, tests, build)
├── app/
│   ├── src/main/java/com/example/musicx/
│   │   ├── catalog/           # YouTube Music InnerTube catalog client
│   │   ├── playback/          # Media3 playback service and controller
│   │   ├── playlist/          # Local playlist and storage repository
│   │   ├── scrobble/          # ListenBrainz scrobbling integration
│   │   ├── spotify/           # Spotify auth, harvesting, and matching
│   │   └── ui/                # Compose screens, components, and ViewModels
│   └── src/test/java/         # Comprehensive unit test suite
├── PRODUCT.md                 # Product specifications and constraints
├── DESIGN.md                  # Visual guidelines and design system
├── CONTRIBUTING.md            # Guidelines for contributors
└── LICENSE                    # Apache 2.0 License
```

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
