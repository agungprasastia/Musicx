# Musicx Milestone 1: Catalog and Playback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace demo-only catalog/player behavior with real guest YouTube Music search, stream resolution, and Media3 background playback.

**Architecture:** Keep Compose UI as presentation only. Add typed catalog and resolver boundaries, then a Media3-owned `MediaSessionService` that owns player/queue and exposes state/control to the Activity. UI submits intents and renders service/catalog states; it never owns a long-lived player or fabricates successful playback.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Media3 ExoPlayer/Session, current Gradle Version Catalog, JUnit 4 and existing Compose instrumentation test dependencies.

**Spec:** `docs/superpowers/specs/2026-11-08-musicx-production-roadmap-design.md` (Milestone 1 only)

## Global Constraints

- Native Musicx implementation, not a BitChord fork.
- Use unofficial YouTube Music guest catalog and stream resolution; disclose instability and independent status.
- Android Gradle Plugin `9.5.1`, Kotlin/Compose plugin `2.2.10`, Gradle `9.6.0`, compile SDK `37`, min SDK `26`.
- Add only Media3 and necessary HTTP/serialization dependencies not already installed; do not copy BitChord's broad dependency set.
- No Spotify, Google Sign-In, downloads, local library scanning, synchronized lyrics, equalizer, crossfade/automix, scrobbling, or Discord in this milestone.
- Never log credentials or full stream URLs; no DRM/paywall/access-control circumvention.
- Treat network responses and URLs as untrusted; validate HTTPS, bound response sizes/timeouts, propagate cancellation, and use typed failures.
- Do not persist expiring stream URLs beyond validity; do not report playing until Media3 player state confirms it.
- JDK 25 at `D:\`; run Gradle from `D:\` using `-p D:\KULIAH\OpenSource\Musicx`; do not change permanent environment variables.

## Review Focus

- Malformed/empty unofficial catalog response must become a typed error or empty state, never a crash or fabricated track; pin in catalog parser tests.
- Expired, rejected, or malformed stream URL must fail current item and permit queue recovery; pin in resolver/playback tests.
- Rapid track selection/service disconnect must not play stale resolved item or leak player; pin in queue/service tests.
- Notification/lock-screen pause, seek, next, and previous must affect the same active player and reflect actual state; pin in MediaSession instrumentation tests.
- App leaving foreground or process recreation must not falsely display playing while service/player is stopped; pin lifecycle tests and manual device validation.

---

## File map

- `gradle/libs.versions.toml`: Media3 and any required networking/serialization version aliases.
- `app/build.gradle.kts`: minimum dependencies and Media3 service configuration only.
- `app/src/main/AndroidManifest.xml`: foreground media playback service declaration and required media permissions.
- `app/src/main/java/com/example/musicx/core/model/MusicItem.kt`: shared catalog/queue domain models.
- `app/src/main/java/com/example/musicx/catalog/youtube/YouTubeMusicClient.kt`: unofficial client transport and endpoint DTOs.
- `app/src/main/java/com/example/musicx/catalog/youtube/YouTubeMusicRepository.kt`: parsing, mapping, search/browse operations.
- `app/src/main/java/com/example/musicx/playback/resolver/StreamResolver.kt`: replaceable track-ID to validated stream-URI boundary.
- `app/src/main/java/com/example/musicx/playback/PlaybackService.kt`: Media3 player/session/service lifecycle and notification state.
- `app/src/main/java/com/example/musicx/playback/PlaybackController.kt`: UI-facing service connection and commands/state.
- `app/src/main/java/com/example/musicx/ui/MusicApp.kt`, `MusicScreens.kt`, `NowPlayingScreen.kt`: connect existing Compose prototype to catalog and playback state.
- Unit tests under `app/src/test/java/com/example/musicx/{catalog,playback}/`; instrumentation tests under existing `app/src/androidTest/java/com/example/musicx/`.

### Task 1: Domain types and YouTube Music catalog client

**Files:**
- Create: `app/src/main/java/com/example/musicx/core/model/MusicItem.kt`
- Create: `app/src/main/java/com/example/musicx/catalog/youtube/YouTubeMusicClient.kt`
- Create: `app/src/main/java/com/example/musicx/catalog/youtube/YouTubeMusicRepository.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/com/example/musicx/catalog/youtube/YouTubeMusicRepositoryTest.kt`

**Interfaces:**
- Produces `sealed interface CatalogItem` with `Track`, `Album`, `Artist`, `Playlist`; all have stable `id`, `title`, artwork URL?, and type-specific metadata.
- Produces `interface YouTubeMusicClient { suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogDto>>; suspend fun browse(id: String): Result<BrowseDto> }`.
- Produces `class YouTubeMusicRepository(private val client: YouTubeMusicClient)` with `suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogItem>>` and `suspend fun browse(id: String): Result<BrowsePage>`.
- Client transport isolated from parser/mapping so tests use pure DTO inputs and no live requests.

- [x] **Step 1: Write parser/mapping tests**

Test valid track/album/artist/playlist results map to their typed domain items, empty result maps to empty list, malformed required IDs/titles are dropped or returned as typed parse failure, and blank query is rejected before transport call.
- [x] **Step 2: Run focused test and observe RED**

Run from PowerShell: `$p='D:\KULIAH\OpenSource\Musicx'; Set-Location D:\; .\KULIAH\OpenSource\Musicx\gradlew.bat -p $p :app:testDebugUnitTest --tests com.example.musicx.catalog.youtube.YouTubeMusicRepositoryTest --no-daemon --console=plain --no-configuration-cache`.
Expected: FAIL because repository/model symbols are missing. (Use standard Windows PowerShell invocation; no edits to permanent `JAVA_HOME`.)
- [x] **Step 3: Add only required client dependencies and define domain/DTO boundaries**

Check existing dependencies first; add HTTP/serialization libraries only if absent. Keep version aliases centralized.
- [x] **Step 4: Implement repository mapping and request validation**

Transport uses cancellable suspend requests, HTTPS only, explicit timeout/response-size limits, and Result-based typed errors. Do not include request cookies or stream URLs in error strings/logs.
- [x] **Step 5: Re-run focused tests**

Expected: all repository mapping tests PASS.

### Task 2: Browse/search Compose states

**Files:**
- Create: `app/src/main/java/com/example/musicx/ui/MusicViewModel.kt`
- Modify: `app/src/main/java/com/example/musicx/ui/MusicScreens.kt`
- Modify: `app/src/main/java/com/example/musicx/ui/MusicApp.kt`
- Modify: `app/src/androidTest/java/com/example/musicx/MusicNavigationTest.kt`

**Interfaces:**
- Consumes `YouTubeMusicRepository` and `CatalogItem` from Task 1.
- Produces `MusicViewModel.search(query: String, filter: CatalogFilter)`, `browse(id: String)`, `uiState: StateFlow<CatalogUiState>` where `CatalogUiState` models `Idle`, `Loading`, `Results`, `Empty`, and `Error`.
- Selecting a `Track` emits callback only; this task does not claim playing until playback service confirms it.

- [x] **Step 1: Add UI tests**

Verify empty-query prompt, loading indicator, result rows, empty state, retryable error state, and Search/Home/Library screen navigation. Use a fake repository injected into ViewModel tests; Compose tests assert user-visible states.
- [x] **Step 2: Run unit/UI compilation tests and observe RED**

Run `:app:testDebugUnitTest` for ViewModel tests and `:app:compileDebugAndroidTestKotlin` for instrumentation source compilation. Expected: missing ViewModel/UI state symbols; run connected instrumentation only when adb device/AVD is available.
- [x] **Step 3: Implement ViewModel and wire real catalog UI**

Replace sample-only Search/browse result paths with repository state. Keep local demo data only for previews/tests; no fake result rows in production search.
- [x] **Step 4: Run focused tests and compile instrumentation sources**

Expected: ViewModel and Compose state tests PASS; test APK sources compile.

### Task 3: Stream resolver boundary

**Files:**
- Create: `app/src/main/java/com/example/musicx/playback/resolver/StreamResolver.kt`
- Create: `app/src/main/java/com/example/musicx/playback/resolver/YouTubeStreamResolver.kt`
- Test: `app/src/test/java/com/example/musicx/playback/resolver/YouTubeStreamResolverTest.kt`

**Interfaces:**
- Produces `data class ResolvedStream(val uri: Uri, val mimeType: String?, val expiresAtEpochMillis: Long?)`.
- Produces `interface StreamResolver { suspend fun resolve(trackId: String): Result<ResolvedStream> }`.
- Resolver returns failure for blank ID, unsupported scheme/host, malformed response, unavailable track, timeout, and upstream rejection. Do not persist resolved stream URL.

- [x] **Step 1: Write resolver validation tests**

Cover valid HTTPS URL, reject HTTP/non-allowlisted host, blank ID, malformed/expired response, timeout, and upstream failure without exposing full URL in error text.
- [x] **Step 2: Run focused test and observe RED**

Run from PowerShell: `$p='D:\KULIAH\OpenSource\Musicx'; Set-Location D:\; .\KULIAH\OpenSource\Musicx\gradlew.bat -p $p :app:testDebugUnitTest --tests com.example.musicx.playback.resolver.YouTubeStreamResolverTest --no-daemon --console=plain --no-configuration-cache`. Expected: resolver types absent.
- [x] **Step 3: Implement isolated YouTube resolver client**

Use replaceable client interface and only supported unofficial stream resolution needed for playable public content; do not implement DRM, paywall, or access-control circumvention.
- [x] **Step 4: Run focused resolver tests**

Expected: tests PASS, stream URL redaction asserted.

### Task 4: Media3 playback service, session, queue, and UI controller

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/example/musicx/playback/PlaybackService.kt`
- Create: `app/src/main/java/com/example/musicx/playback/PlaybackController.kt`
- Create: `app/src/main/java/com/example/musicx/playback/PlaybackQueue.kt`
- Modify: `app/src/main/java/com/example/musicx/ui/MusicViewModel.kt`
- Modify: `app/src/main/java/com/example/musicx/ui/MusicApp.kt`
- Modify: `app/src/main/java/com/example/musicx/ui/NowPlayingScreen.kt`
- Test: `app/src/test/java/com/example/musicx/playback/PlaybackQueueTest.kt`
- Test: `app/src/androidTest/java/com/example/musicx/PlaybackSessionTest.kt`

**Interfaces:**
- Consumes `CatalogItem.Track` and `StreamResolver`.
- Produces `PlaybackQueue` operations `setQueue(tracks: List<CatalogItem.Track>, startIndex: Int)`, `append(track)`, `remove(index)`, `move(from, to)`, `next()`, `previous()`; invalid indexes leave queue unchanged and return false.
- Produces service-bound `PlaybackController` `StateFlow<PlaybackUiState>` and commands `play(track)`, `enqueue(track)`, `playPause()`, `seekTo(positionMs)`, `skipNext()`, `skipPrevious()`, `release()`.
- `PlaybackUiState` distinguishes idle, resolving, ready/playing, paused, ended, and error; current track and position/duration come from Media3 state.

- [x] **Step 1: Write queue transition unit tests**

Test set queue/start index, append, next/previous bounds, remove current/other, move, and empty queue.
- [x] **Step 2: Run queue tests and observe RED**

Expected: `PlaybackQueue` missing.
- [x] **Step 3: Implement queue model and run tests**

Expected: all queue transition tests PASS.
- [x] **Step 4: Add Media3 aliases/dependencies and manifest service**

Add compatible Media3 ExoPlayer, Session, Common versions in catalog; service declares foreground service type `mediaPlayback` and only required permissions. No unverified media library additions.
- [x] **Step 5: Implement `PlaybackService` and controller**

Service exclusively owns ExoPlayer and MediaSession; resolver runs before adding playable MediaItem; release player/session on service destruction. Notification and lock-screen transport commands route through MediaSession. Audio focus and noisy-output handling use Media3 defaults.
- [x] **Step 6: Connect Compose controls to controller state**

Remove demo-play toggles/static timeline for production screen. Mini-player, Now Playing, queue, progress, and controls reflect actual `PlaybackUiState`; resolving/error states are visible and cannot say playing.
- [x] **Step 7: Add instrumentation tests and run available checks**

Test service session commands and notification metadata where emulator/device exists. Always run `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:assembleDebug`. Connected tests report blocked if no adb device; do not claim them passed.

Result: `PlaybackSessionTest` added and compiles; connected runtime remains BLOCKED (no adb device/AVD).

### Task 5: End-to-end catalog-to-background playback acceptance

**Files:**
- Modify: `app/src/androidTest/java/com/example/musicx/PlaybackSessionTest.kt`
- Modify only affected catalog/playback/UI files as tests require.

- [x] **Step 1: Add end-to-end fake-resolver test**

Select catalog track → queue → resolver succeeds → Media3 prepares; assert state transitions resolving→ready and title/queue metadata match selected track. Resolver failure must reach error and never ready/playing.

`PlaybackSessionTest` covers both paths with a fake `StreamResolver` and a local data-URI WAV; compiled but not executed (no device).
- [ ] **Step 2: Run test and observe expected failure if integration missing**

BLOCKED: no adb device/AVD available; test cannot be executed in this environment.
- [ ] **Step 3: Fix smallest integration defect without adding unrelated features**

BLOCKED: depends on Step 2 runtime observation.
- [x] **Step 4: Run final local suite/build**

Run from PowerShell: `$p='D:\KULIAH\OpenSource\Musicx'; Set-Location D:\; .\KULIAH\OpenSource\Musicx\gradlew.bat -p $p :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug --no-daemon --console=plain --no-configuration-cache`.
Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 5: Manual device acceptance**

On physical Android device: play a resolvable public track, background app, lock screen, test notification pause/next/seek, audio focus interruption, unavailable stream behavior, and service cleanup. Record device-only results separately from automated tests.

BLOCKED: no physical device/emulator. Additionally, live guest `/player` currently returns `LOGIN_REQUIRED` from this network, so a resolvable public track may require sign-in.

## Deferred after Milestone 1

Spotify session/client/playlists/matching, Google Sign-In, offline/local media, synchronized lyrics, dynamic artwork color, equalizer/speed/sleep timer/skip silence/crossfade/automix, scrobbling, Discord, release signing, visual redesign beyond required playback flows.
