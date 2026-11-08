# Musicx Milestone 4: Local, Offline, and Playback Controls

**Goal:** Add local audio library scanning, playback speed control, sleep timer, skip silence, system equalizer launch. Crossfade and offline downloads deferred.

## Tasks

### Task 1: Local audio library scanning

- [x] **Step 1:** `local/LocalAudioScanner.kt` — MediaStore query (`IS_MUSIC != 0`), `mapRow` rejects blank title/invalid id.
- [x] **Step 2:** `local/LocalLibraryRepository.kt` — IO-dispatched scan.
- [x] **Step 3:** `local/LocalAwareStreamResolver.kt` — `content://media/external/audio/media/*` ids play directly; others go to YouTube resolver.
- [x] **Step 4:** Library tab requests `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (API ≤32, `maxSdkVersion=32`).
- [x] **Step 5:** `LocalAudioScannerTest` (5 tests).

### Task 2: Playback speed

- [x] `PlaybackController.setSpeed` via `MediaController.playbackParameters`; chips 0.5x–2.0x in Now Playing.

### Task 3: Sleep timer

- [x] `startSleepTimer`/`cancelSleepTimer` using `SystemClock.elapsedRealtime` deadline; presets 15/30/45/60/90 min.
- Limitation: timer lives in controller scope; does not survive Activity destruction. Move into `PlaybackService` if needed.

### Task 4: Skip silence and equalizer

- [x] Skip silence: `MediaController` has no `skipSilenceEnabled`, so `PlaybackService` exposes custom session command `com.example.musicx.SKIP_SILENCE`, granted only to own package.
- [x] Equalizer: `AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL`; Toast when no EQ app.

### Task 5: Crossfade

- [ ] **DEFERRED** — Media3 has no native crossfade between queue items; needs dual-player or custom audio processor.

### Task 6: Offline downloads

- [ ] **BLOCKED** — requires source permissions review and platform terms compliance check.

### Task 7: Verification

- [x] `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug` PASS (115 unit tests, 0 failures).
- [ ] **BLOCKED** — instrumentation/manual test on device (no adb device/AVD): permission flow, local playback, speed, skip silence, sleep timer, EQ launch.
