# Musicx Milestone 2: Spotify Library Integration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an isolated Spotify WebView session, private playlist/Liked Songs reading, and deterministic YouTube Music matching. Spotify supplies metadata only; playback always goes through the Milestone 1 YouTube Music resolver and Media3 service.

**Architecture:** Keep Spotify in `spotify/` with no dependency on Compose or Media3. `spotify/auth` owns the `sp_dc` session, encrypted storage, token harvesting, and `SessionState`. `spotify/net` owns the private HTTP/GraphQL client and parsers. `spotify/match` is a pure, deterministic matcher over `CatalogItem.Track` candidates. `ui/spotify` renders session/library/match state and sends the confirmed `CatalogItem.Track` to the existing `PlaybackController`.

**Spec:** `docs/superpowers/specs/2026-11-08-musicx-production-roadmap-design.md` (Milestone 2 only)

## Global Constraints

- Native Musicx implementation; do not copy BitChord source. Protocol facts (hosts, operation names, persisted-query hashes) may be re-derived, but code must be written independently.
- Spotify audio is never streamed or downloaded. Only playlist/Liked Songs metadata is read.
- `sp_dc` is bearer-equivalent. Never log, print, export, or pass it to JavaScript or UI diagnostics. Store only in Android Keystore-encrypted app-private storage; exclude from cloud backup and device transfer; clear on disconnect.
- WebView navigation is limited to required Spotify login/player origins and named identity providers. Unknown top-level navigations are refused. No `addJavascriptInterface` in the login WebView. The token-harvest WebView exposes only a native sink that accepts the page's own `/api/token` payload; it never receives the cookie.
- Validate HTTPS and exact/subdomain host boundaries for every request. Bound response sizes, set timeouts, propagate cancellation, and use typed errors with constant messages.
- Spotify failures must never break YouTube guest catalog or playback. Expired sessions ask for reconnect.
- No DRM/paywall/access-control bypass. Do not forge TOTP tokens; harvest only the token the real web player mints for its own session.
- Show unresolved or ambiguous matches; never silently choose an uncertain YouTube Music track.
- Add no dependency unless a compatibility/need check is documented first. Prefer the current OkHttp/serialization stack and platform `WebView`/Keystore APIs.

## File map

- `app/src/main/java/com/example/musicx/spotify/model/SpotifyModels.kt`: playlist/track/session/error types.
- `app/src/main/java/com/example/musicx/spotify/auth/SpotifySecret.kt`: redacted secret wrapper and cookie parsing.
- `app/src/main/java/com/example/musicx/spotify/auth/SpotifySecretStore.kt`: encrypted Keystore store plus in-memory fake for tests.
- `app/src/main/java/com/example/musicx/spotify/auth/SpotifyTokenParser.kt`: `/api/token` and client-token parsers.
- `app/src/main/java/com/example/musicx/spotify/auth/SpotifyTokenProvider.kt`: cached bearer/client tokens, harvest orchestration, expiry.
- `app/src/main/java/com/example/musicx/spotify/auth/SpotifyWebTokenHarvester.kt`: offscreen WebView harvest and `SpotifyLoginWebView` policy.
- `app/src/main/java/com/example/musicx/spotify/net/SpotifyHttpTransport.kt`: HTTPS-only OkHttp transport with bounded bodies.
- `app/src/main/java/com/example/musicx/spotify/net/SpotifyLibraryClient.kt`: private library GraphQL requests and pure parsers.
- `app/src/main/java/com/example/musicx/spotify/SpotifyRepository.kt`: session/library orchestration boundary.
- `app/src/main/java/com/example/musicx/spotify/SpotifyViewModel.kt`: session, library, playlist, and match UI state.
- `app/src/main/java/com/example/musicx/spotify/match/SpotifyTrackMatcher.kt`: deterministic scoring and ambiguity decisions.
- `app/src/main/java/com/example/musicx/ui/SpotifyScreens.kt` and `SpotifyLoginScreen.kt`: sign-in overlay, library, playlist, match review.
- `app/src/main/java/com/example/musicx/ui/MusicApp.kt`: Spotify tab, ViewModel wiring, login overlay, playback handoff.
- `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`: exclude `spotify_session.xml`.
- Unit tests under `app/src/test/java/com/example/musicx/spotify/`; instrumentation sources under `app/src/androidTest/java/com/example/musicx/`.

### Task 1: Spotify models, cookie/token validation, and deterministic matching

**Files:**
- Create: `spotify/model/SpotifyModels.kt`
- Create: `spotify/auth/SpotifySecret.kt`
- Create: `spotify/auth/SpotifyTokenParser.kt`
- Create: `spotify/match/SpotifyTrackMatcher.kt`
- Test: `app/src/test/java/com/example/musicx/spotify/SpotifyAuthParsingTest.kt`
- Test: `app/src/test/java/com/example/musicx/spotify/SpotifyTrackMatcherTest.kt`

**Interfaces:**
- `SpotifyPlaylist(id, name, owner, imageUrl, trackCount)`.
- `SpotifyTrack(id, title, artists, album, durationMs, imageUrl)`.
- `SpotifySecret` wraps `sp_dc`; `toString()` is always redacted.
- `parseSpDcCookie(raw: String?): String?` accepts only a non-blank, control-free, semicolon-free value from the `sp_dc` cookie pair.
- `parseWebToken(json, now): Result<SpotifyWebToken>` rejects anonymous, blank, expired, or whitespace-bearing tokens.
- `parseClientToken(json): Result<SpotifyClientToken>` accepts only `RESPONSE_GRANTED_TOKEN_RESPONSE`.
- `SpotifyTrackMatcher.rank(track, candidates): List<TrackMatch>` and `decide(...): MatchDecision` (`Matched`, `Ambiguous`, `NoMatch`).
- Scoring normalizes case, diacritics, punctuation, `&`, and `feat.`; weights title 0.58, artist 0.27, album 0.08, duration 0.07, renormalized when album/duration are absent. Default threshold 0.72 and ambiguity margin 0.08.

- [x] **Step 1: Write parsing and matcher tests**

Cover valid/malformed `sp_dc`, duplicate/blank cookies, redacted `toString`, valid/anonymous/expired/whitespace tokens, granted/rejected client tokens, exact match, `feat.`/punctuation/diacritic variants, artist mismatch, duration tie-break, ambiguity, empty candidates, and below-threshold results.
- [x] **Step 2: Run focused tests and observe RED**

Run `:app:testDebugUnitTest --tests "com.example.musicx.spotify.*"`. Expected: Spotify types absent.
- [x] **Step 3: Implement models, parsers, and matcher**

No network or Android dependencies in this task; everything must run on the JVM.
- [x] **Step 4: Run focused tests**

Expected: all Task 1 tests PASS.

### Task 2: Encrypted session storage and backup exclusion

**Files:**
- Create: `spotify/auth/SpotifySecretStore.kt`
- Modify: `app/src/main/res/xml/backup_rules.xml`
- Modify: `app/src/main/res/xml/data_extraction_rules.xml`
- Test: `app/src/test/java/com/example/musicx/spotify/SpotifySecretStoreTest.kt`
- Test (compiled only): `app/src/androidTest/java/com/example/musicx/SpotifySecretStoreInstrumentedTest.kt`

**Interfaces:**
- `interface SpotifySecretStore { fun loadSecret(): SpotifySecret?; fun saveSecret(secret: SpotifySecret); fun clear() }`.
- `EncryptedSpotifySecretStore(context)` uses an Android Keystore AES/GCM key (`musicx.spotify.sp_dc`), stores only base64 IV/ciphertext in `spotify_session` preferences, and clears corrupt/undecryptable entries.
- `InMemorySpotifySecretStore` for JVM/ViewModel tests.

- [x] **Step 1: Write store contract tests and observe RED**

Verify fake-store contract, blank/corrupt secret handling, redaction, and that no API exposes the raw value through `toString`. Keystore round-trip belongs in instrumentation.
- [x] **Step 2: Implement encrypted store and backup exclusions**

Exclude `spotify_session.xml` from full backup, cloud backup, and device transfer. Do not disable backup globals.
- [x] **Step 3: Compile instrumentation source**

`SpotifySecretStoreInstrumentedTest` performs save/load/clear and asserts the preferences file contains neither the raw cookie nor plaintext. Runtime is BLOCKED without an adb device.

### Task 3: Private token provider and Spotify library client

**Files:**
- Create: `spotify/auth/SpotifyTokenProvider.kt`
- Create: `spotify/auth/SpotifyWebTokenHarvester.kt`
- Create: `spotify/net/SpotifyHttpTransport.kt`
- Create: `spotify/net/SpotifyLibraryClient.kt`
- Test: `app/src/test/java/com/example/musicx/spotify/SpotifyTokenProviderTest.kt`
- Test: `app/src/test/java/com/example/musicx/spotify/SpotifyLibraryClientTest.kt`

**Interfaces:**
- `SpotifyTokenProvider(secretStore, harvester, clientTokenSource, clock)` caches `SpotifyTokens(bearer, clientToken, expiresAtEpochMillis)` and refreshes 30 seconds before expiry.
- Missing cookie → `SpotifyError.NotConnected`; rejected/anonymous harvest → `SpotifyError.SessionExpired`; network/parse failures are typed and redacted.
- `SpotifyHttpTransport` allowlists `https://open.spotify.com`, `https://accounts.spotify.com`, `https://clienttoken.spotify.com`, and `https://api-partner.spotify.com`; disables redirects; caps bodies at 2 MiB.
- `SpotifyLibraryClient.playlists()`, `playlistTracks(id)`, `likedTracks()` page through partner GraphQL with bounded pages and return typed failures.
- Pure parsers map `libraryV3`, `fetchPlaylist`, and `fetchLibraryTracks` fixtures; malformed entries are dropped, never fabricated.
- Token harvest uses an offscreen WebView with JavaScript enabled, third-party cookies, a timeout, and a native bridge that receives only `/api/token` response bodies. The cookie is injected through `CookieManager`; it is never handed to JavaScript.

- [x] **Step 1: Write token-provider and parser tests and observe RED**

Library parser fixture tests are written; parser tests are not complete until bounded pagination and malformed-entry cases are covered.

Cover cache reuse, refresh-near-expiry, missing cookie, anonymous-only harvest, rejection mapping, pagination bounds, dropped malformed items, and no token in error text.
- [x] **Step 2: Implement transport, parsers, and token provider**

Token cache, HTTPS host validation, fixture parsers, bounded pagination client, and typed error mapping implemented.

Document that partner GraphQL operation hashes are protocol constants and may change upstream; surface `SpotifyError.SessionExpired`/`ApiChanged` instead of crashing.
- [x] **Step 3: Implement offscreen harvester and login policy**

No JS interface in the login screen. Unknown login navigations are refused. Harvest WebView is destroyed and storage cleared in `finally`.
- [x] **Step 4: Run focused tests**

All Task 3 tests PASS; instrumentation sources compile.

### Task 4: Repository, session recovery, and match orchestration

**Files:**
- Create: `spotify/SpotifyRepository.kt`
- Test: `app/src/test/java/com/example/musicx/spotify/SpotifyRepositoryTest.kt`

**Interfaces:**
- `SpotifyRepository` exposes `StateFlow<SpotifySessionState>` and `connect(secret)`, `disconnect()`, `playlists()`, `playlistTracks(id)`, `likedTracks()`.
- Session states: `Disconnected`, `Connecting`, `Connected`, `Expired`, `Error(message)`.
- Connect stores the secret only after token validation succeeds; failed validation clears it.
- Disconnect clears the Keystore entry, token cache, and Spotify WebView cookies without touching YouTube cookies.
- Repository maps 401/403 and rejected harvest to `Expired`; all other failures keep YouTube catalog/playback independent.

- [x] **Step 1: Write repository tests with fakes and observe RED**

Cover successful connect, failed connect clears the secret, restore with stored secret, expiry transition, disconnect clearing, and library failure not changing session state.
- [x] **Step 2: Implement repository**
- [x] **Step 3: Run focused tests**

All repository tests PASS.

### Task 5: Spotify UI, login WebView, library and match review

**Files:**
- Create: `ui/SpotifyLoginScreen.kt`
- Create: `ui/SpotifyScreens.kt`
- Modify: `ui/MusicApp.kt`
- Test (compiled only): `app/src/androidTest/java/com/example/musicx/SpotifyUiTest.kt`

**Interfaces:**
- A `Spotify` bottom tab shows disconnected, loading, library, playlist, and error states.
- Connect opens a full-screen WebView login overlay. On success, the overlay closes and the playlist list loads.
- Playlist rows open a track list; each Spotify track has a "Find match" action.
- Match flow: YouTube Music song search for `title + primary artist` → matcher → `Matched`, `Ambiguous` (top candidates), or `NoMatch`.
- Only an explicitly chosen/match-confirmed `CatalogItem.Track` is sent to `PlaybackController.play`. No auto-play on ambiguous results.
- Disconnect clears the session and returns to disconnected copy.

- [x] **Step 1: Add Compose instrumentation sources and compile**

Instrumentation stubs compile. Runtime BLOCKED without a device.
- [x] **Step 2: Implement login overlay and Spotify screens**
- [x] **Step 3: Wire the Spotify tab and playback handoff**

Existing Home/Search/Library flows and mini-player unchanged.
- [x] **Step 4: Compile and run all available checks**

Unit suite PASS, `:app:compileDebugAndroidTestKotlin` PASS, `:app:assembleDebug` PASS.

### Task 6: Verification and honest reporting

- [x] **Step 1: Run the full local suite**

`:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:assembleDebug` — BUILD SUCCESSFUL.

- [x] **Step 2: Record results in the SDD ledger**

No live Spotify account available. Login, private API responses, token harvesting, WebView behavior, and device playback are unverified. Instrumentation tests compile but runtime is BLOCKED.
- [ ] **Step 3: Manual device acceptance**

Requires a real Spotify account and device. BLOCKED in this environment.
