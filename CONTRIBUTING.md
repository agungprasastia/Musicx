# Contributing to Musicx

Thank you for contributing to Musicx! This document outlines guidelines and workflows for contributing code and reports to this project.

---

## Code of Conduct

- Be respectful and collaborative.
- Provide clear context and reproduction steps for issues.

---

## Development Workflow

### Requirements

- Android Studio Ladybug (or newer)
- JDK 17 (or newer)
- Android SDK 35/37 (Android 10+ minimum supported runtime, `minSdk = 26`)

### Branching Strategy

- `main` / `master` is the stable production branch.
- Feature branches follow: `feat/<feature-name>`, `fix/<bug-name>`, or `refactor/<target>`.

### Building and Testing

Run tests and checks locally before pushing or creating pull requests:

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run Android lint
./gradlew lintDebug

# Build debug APK
./gradlew assembleDebug
```

All 125+ unit tests and lint checks must pass.

---

## Code Conventions

- **Language:** Kotlin with standard Kotlin Coding Conventions.
- **UI Architecture:** Jetpack Compose + Material 3, Single Activity architecture (`MainActivity`).
- **State Management:** MVI / MVVM with unidirectional data flow via `StateFlow` and Compose state hoisting.
- **Audio Engine:** AndroidX Media3 (`MediaSessionService`, `ExoPlayer`), background playback, lock-screen notifications.
- **Network / Transport:** OkHttp with structured JSON serialization (`kotlinx.serialization`).
- **Security:** Secrets (Spotify cookies, scrobbler tokens) must be saved into Android Keystore-backed encrypted storage (`EncryptedSpotifySecretStore`, `ListenBrainzTokenStore`). Never log secrets or tokens.

---

## Pull Request Checklist

Before submitting a PR:

- [ ] Code compiles cleanly without errors.
- [ ] `./gradlew lintDebug` passes with 0 errors.
- [ ] `./gradlew testDebugUnitTest` passes all tests.
- [ ] No extraneous debug files, logs, or secrets included in the diff.
- [ ] Commit messages are descriptive and concise.
