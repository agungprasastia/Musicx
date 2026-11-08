# Musicx Milestone 5: Integrations and Finishing

**Goal:** Scrobbling via ListenBrainz, Settings screen, accessibility pass, release signing configuration, stub/blocked state documentation for Last.fm/Discord presence.

## Tasks

### Task 1: Scrobbling and integrations

- [x] **Step 1: ListenTracker** (`scrobble/ListenTracker.kt`)
  - Track duration > 30s.
  - Submits after 50% duration or 4 minutes (ListenBrainz / Last.fm rule).
  - Paused time does not count; tick intervals capped at 2s.
  - Generates valid ListenBrainz JSON payload.
- [x] **Step 2: ListenBrainzClient** (`scrobble/ListenBrainzClient.kt`)
  - Validates token against `https://api.listenbrainz.org/1/validate-token`.
  - Submits single listen to `https://api.listenbrainz.org/1/submit-listens`.
  - Enforces HTTPS, token format, and body read bounds.
- [x] **Step 3: ListenBrainzTokenStore** (`scrobble/ListenBrainzTokenStore.kt`)
  - AES/GCM encryption via Android Keystore.
  - Backups and device transfer excluded in `backup_rules.xml` and `data_extraction_rules.xml`.
- [x] **Step 4: ListenTrackerTest** (`app/src/test/java/com/example/musicx/scrobble/ListenTrackerTest.kt`)
  - 10 unit tests covering threshold, pause behavior, unknown artist skip, token format, schema.
- [x] **Step 5: Settings UI** (`ui/SettingsScreen.kt`)
  - ListenBrainz connect / disconnect.
  - Clear messaging on Last.fm (no API key configured) and Discord presence (no Discord application configured).
- [ ] **Last.fm scrobbling: BLOCKED** — requires registered Last.fm API key and secret.
- [ ] **Discord presence: BLOCKED** — requires Discord application ID and IPC client.

### Task 2: Accessibility and visual refinement

- [x] Touch targets ≥ 48dp on navigation, buttons, rows, and player controls.
- [x] Semantic roles (`Role.Button`, `Role.Tab`) and headings (`heading()`) added across screens.
- [x] Contrast and status indicators clear across light/dark themes.

### Task 3: Release signing and update process

- [x] `app/build.gradle.kts` release signingConfig configured with environment / project properties (`MUSICX_RELEASE_STORE_FILE`, `MUSICX_RELEASE_STORE_PASSWORD`, `MUSICX_RELEASE_KEY_ALIAS`, `MUSICX_RELEASE_KEY_PASSWORD`).
- [x] `buildConfig = true` enabled in `buildFeatures`.

### Task 4: Verification

- [x] Unit tests pass (`:app:testDebugUnitTest`).
- [x] Android test compilation passes (`:app:compileDebugAndroidTestKotlin`).
- [x] Debug APK builds cleanly (`:app:assembleDebug`).
- [ ] **Connected device tests: BLOCKED** — no adb device or emulator available.
