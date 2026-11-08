# Musicx UI Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace starter greeting with polished editorial-dark Compose music UI prototype.

**Architecture:** Keep one Activity and use local Compose state for selected tab, sample track, and demo play state. Split sample data and focused screen components into small Kotlin files; use existing Material 3 and Compose dependencies, no navigation or image library.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose, Material 3, JUnit 4, Compose UI test.

**Spec:** `docs/superpowers/specs/2026-11-08-musicx-ui-prototype-design.md`

## Global Constraints

- Visual direction: editorial dark, inspired by Apple Music and BitChord without copying assets or exact UI.
- Use local sample content and demo interactions only.
- Home, Search, and Library use bottom navigation; persistent mini-player appears above it.
- Now Playing is art-first with large artwork, track info, progress display, playback controls, and queue/lyrics affordances.
- No YouTube Music access/streaming, audio playback, Google Sign-In, background playback, system controls, downloads, local scanning, synchronized lyrics, smart transitions, equalizer, scrobbling, or Discord presence.
- Do not imply deferred capabilities are active; do not simulate synchronized lyrics or real audio progress.
- Use existing dependencies; add none.
- Keep dark palette fixed; do not switch to system light colors or Android dynamic colors.

## Review Focus

- Switching tabs must keep mini-player visible and selected tab correct; cover with UI test.
- Mini-player tap must open Now Playing; cover with UI test.
- Demo play/pause must update only its demo label/icon and must not claim real playback; cover with UI test.
- Empty search input must show a useful prompt rather than a blank results state; cover with UI test.
- Small viewport/content overflow must not hide primary controls; use scrollable screen content and verify layout in emulator/manual review.

---

### Task 1: Fixed editorial theme and sample catalog

**Files:**
- Modify: `app/src/main/java/com/example/musicx/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/example/musicx/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/musicx/data/MusicSample.kt`
- Test: `app/src/test/java/com/example/musicx/data/MusicSampleTest.kt`

**Interfaces:**
- Produces `data class MusicSample(id: String, title: String, artist: String, album: String, durationLabel: String, artworkColors: List<Long>)` and `object MusicSamples` with `tracks`, `playlists`, and `albums` sample lists.
- Produces fixed near-black/charcoal/white/gray/accent theme tokens and `MusicxTheme` that always uses the fixed dark color scheme.

- [ ] **Step 1: Add sample catalog test**

Create `MusicSampleTest` asserting sample track list is non-empty, IDs are unique, and each item has nonblank title/artist plus at least two artwork colors.

- [ ] **Step 2: Run test and confirm expected failure**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.example.musicx.data.MusicSampleTest'`
Expected: FAIL because `MusicSamples` is not defined.

- [ ] **Step 3: Implement sample data and fixed dark theme**

Add a compact handful of sample tracks/playlists/albums with local artwork gradients represented by color values. Update theme to disable dynamic/system light colors and use stable dark tokens.

- [ ] **Step 4: Run focused test**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.example.musicx.data.MusicSampleTest'`
Expected: PASS.

### Task 2: Home, Search, and Library screens

**Files:**
- Create: `app/src/main/java/com/example/musicx/ui/MusicScreens.kt`
- Create: `app/src/main/java/com/example/musicx/ui/MusicComponents.kt`
- Test: `app/src/androidTest/java/com/example/musicx/MusicNavigationTest.kt`

**Interfaces:**
- Consumes `MusicSamples` and fixed theme tokens from Task 1.
- Produces `enum class MusicTab { Home, Search, Library }` and composables `HomeScreen(onTrackSelected: (MusicSample) -> Unit)`, `SearchScreen(onTrackSelected: (MusicSample) -> Unit)`, and `LibraryScreen(onTrackSelected: (MusicSample) -> Unit)`.
- Produces reusable artwork, section heading, track row, and mini-player composables in `MusicComponents.kt`.

- [ ] **Step 1: Add screen navigation UI test**

Test opening Home, tapping Search then Library tabs, assert each screen heading/content is displayed, and assert mini-player remains visible.

- [ ] **Step 2: Run UI test and confirm expected failure**

Run: `./gradlew :app:connectedDebugAndroidTest --tests 'com.example.musicx.MusicNavigationTest'`
Expected: FAIL because navigation UI does not exist.

- [ ] **Step 3: Implement screens and reusable components**

Home shows greeting, featured mixes, and recently played/recommended content. Search shows query field, helpful empty-query prompt, and sample categories/results. Library organizes playlists, albums, artists, and songs. Make screen content vertically scrollable; use accessible text labels and touch-sized controls.

- [ ] **Step 4: Run UI test**

Run: `./gradlew :app:connectedDebugAndroidTest --tests 'com.example.musicx.MusicNavigationTest'`
Expected: PASS.

### Task 3: Now Playing, mini-player, and app integration

**Files:**
- Modify: `app/src/main/java/com/example/musicx/MainActivity.kt`
- Create: `app/src/main/java/com/example/musicx/ui/NowPlayingScreen.kt`
- Modify: `app/src/main/java/com/example/musicx/ui/MusicComponents.kt`
- Modify: `app/src/androidTest/java/com/example/musicx/MusicNavigationTest.kt`

**Interfaces:**
- Consumes screen composables and `MusicSamples` from earlier tasks.
- Produces `MusicxApp()` as the Activity content root; local state tracks selected tab, selected sample track, and demo-playing Boolean.
- `NowPlayingScreen(track: MusicSample, isDemoPlaying: Boolean, onPlayPause: () -> Unit, onBack: () -> Unit)` presents static artwork and nonfunctional progress display; queue and lyrics controls are clearly demo affordances.

- [ ] **Step 1: Add mini-player and demo-control UI tests**

Assert mini-player opens Now Playing; assert play/pause changes only the displayed demo state, with an explicit `Demo` label visible. Assert no real-time progress claim is present.

- [ ] **Step 2: Run tests and confirm expected failure**

Run: `./gradlew :app:connectedDebugAndroidTest --tests 'com.example.musicx.MusicNavigationTest'`
Expected: FAIL because Now Playing and demo controls are not wired.

- [ ] **Step 3: Implement Now Playing and wire MusicxApp**

Replace `Greeting` in Activity content with `MusicxApp`. Keep bottom tabs on Home/Search/Library, mini-player above tabs, and mini-player tap opening art-first Now Playing. Add gradient sample artwork, title/artist, static progress bar, large demo control, queue/lyrics affordances, and visible prototype labeling. Avoid audio/network APIs and progress timers.

- [ ] **Step 4: Run UI tests and Android build**

Run: `./gradlew :app:connectedDebugAndroidTest :app:testDebugUnitTest :app:assembleDebug`
Expected: all requested tests PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 5: Review final diff and deferred-feature boundaries**

Confirm no new dependencies, audio/network integration, system media claims, or unrelated local configuration changes. If emulator available, inspect Home/Search/Library/Now Playing at phone viewport and verify artwork and controls remain legible.
