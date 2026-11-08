# Musicx Production Roadmap Design

## Goal and intent

Turn the existing Jetpack Compose UI prototype into a usable Android music client, using BitChord as a workflow/feature reference while keeping Musicx implementation native and independently maintainable. Target Android app must provide YouTube Music catalog search/browse and playback, background playback with system controls, and Spotify playlist access that resolves tracks through YouTube Music.

User-approved direction:
- Native Musicx implementation, not a BitChord fork.
- Jetpack Compose UI and Media3 playback/session service.
- Unofficial YouTube Music catalog and stream-resolution client, with known instability and terms-of-service/legal risks.
- Spotify private WebView session and private API flow modeled after BitChord's behavior, with `sp_dc` treated as a sensitive credential.
- Spotify playlist tracks are matched against YouTube Music; Spotify audio itself is not streamed.
- Google Sign-In is deferred.
- Open-source licensing acceptable; project license target is GPLv3. BitChord source remains reference only; do not copy code without explicit license compliance and attribution review.

## Delivery roadmap

### Milestone 1: Real catalog and playback foundation

Deliver in this independently testable milestone:
- Replace sample-only YouTube Music search with an isolated unofficial catalog client and typed domain mapping for songs, albums, artists, and playlists.
- Implement search and basic browse/detail navigation in Compose with loading, empty, and error states.
- Implement an isolated stream resolver from YouTube Music track ID to playable stream URL. Errors must retain actionable source context; never fabricate URLs or report playing until Media3 confirms playback state.
- Add Media3 ExoPlayer and a `MediaSessionService` for queue, foreground/background playback, notification controls, lock-screen controls, audio focus, and lifecycle cleanup.
- Keep UI state driven from playback service/player state; support selecting catalog results, queueing, play/pause, next/previous, seek, and recovery after transient resolution failures.
- Use guest access for YouTube Music in this milestone. Defer YouTube account personalization and Google Sign-In.

Acceptance: search results are real metadata; a selected resolvable track plays through Media3; playback continues after Activity leaves foreground; notification and lock-screen controls change the actual player; failures render real error state and do not create false playback state.

### Milestone 2: Spotify library integration

- Add isolated Spotify WebView login and private playlist/Liked Songs client, as explicitly chosen by user.
- Keep this integration separate from YouTube catalog and playback.
- Match Spotify tracks by title/artist (and album/duration as supporting signals) to YouTube Music candidates; show unresolved/ambiguous matches instead of silently selecting an incorrect song.
- Queue/play only the selected YouTube Music match through Milestone 1 playback path.
- Add disconnect/logout and credential expiry/error handling.

Acceptance: signed-in Spotify playlist and Liked Songs load; user can inspect/resolve ambiguous matches; selected matched YouTube Music song plays via Media3; Spotify session failure never leaks credentials or breaks YouTube guest playback.

### Milestone 3: Accounts and listening experience

- Google Sign-In / YouTube Music account personalization after guest catalog and playback are stable.
- Now Playing polish: artwork transitions, artwork-driven dynamic color, lyrics providers and synchronized highlighting where timing data exists, queue affordances.
- Preserve explicit unavailable states when artwork/lyrics are absent.

### Milestone 4: Local, offline, and playback controls

- Local audio library scanning and local playback.
- Offline downloads only where source permissions and applicable platform terms allow; no hidden extraction or cache presented as an authorized download.
- Speed, sleep timer, skip silence, system equalizer, and crossfade; automix only after transition feasibility/quality is demonstrated.

### Milestone 5: Integrations and finishing

- Last.fm and ListenBrainz scrobbling, Discord presence, accessibility and visual refinement, device coverage, release signing and update process.

## Architecture

Keep one Android app and introduce small native boundaries:

- `catalog/youtube`: unofficial YouTube Music client, DTO-to-domain mapping, catalog repository; all network calls suspendable and cancellable.
- `playback/resolver`: maps a YouTube Music track to a stream URI and metadata. Resolver implementation replaceable independently of UI/player.
- `playback`: Media3 player ownership, queue coordination, playback state, and Android `MediaSessionService`. Activity/Compose never owns the long-lived player.
- `spotify`: WebView login/session lifecycle, private playlist API client, track matching and match review. No stream resolution here.
- `ui`: Compose screens and ViewModels observe repository/player state; no private API calls in composables.
- `core/model`: minimal shared song/album/artist/playlist/queue models and explicit source IDs.

Use existing version catalog and dependencies first. Add only Media3 and required HTTP/serialization libraries that do not already exist, choosing versions compatible with current AGP 9.5.1, Kotlin/Compose plugin 2.2.10, Gradle 9.6.0, compile SDK 37, and min SDK 26. Do not copy BitChord's broad dependency set. Keep playback logic behind interfaces to permit client replacement if unofficial endpoints change.

## Data flow

YouTube path: Compose search/browse → ViewModel → YouTube catalog repository → typed track result → user selection → stream resolver → Media3 queue/session → player state back to ViewModel/UI.

Spotify path: explicit login → session credential store → Spotify playlist client → Spotify track metadata → YouTube Music match candidates → user selection/confirmation → existing YouTube resolver → existing Media3 queue/session.

No direct Spotify stream path. No credential or full stream URL in logs, analytics, crash reports, or UI diagnostics.

## Security, privacy, and failure behavior

- The Spotify `sp_dc` cookie is bearer-equivalent sensitive data. Store only in encrypted app-private storage protected by Android Keystore; exclude from Android backup/device transfer; never log or export it; clear it on disconnect/logout; do not expose it to JavaScript bridges or arbitrary WebView navigation.
- Restrict Spotify WebView navigation to required Spotify account/player origins; external links leave the login flow safely. Validate HTTPS and exact/subdomain host boundaries.
- Do not store Google credentials in Milestone 1; later Google sign-in must use supported OAuth/OIDC flows and least-privilege scopes.
- Treat all unofficial endpoint responses and stream URLs as untrusted input. Enforce HTTPS, sane response sizes/timeouts, cancellation, and typed errors. Avoid persisting expiring stream URLs beyond their valid lifetime.
- Handle expired Spotify sessions by asking user to reconnect; allow YouTube guest playback to continue independently.
- Be clear in app information that YouTube Music/Spotify integrations are unofficial and not endorsed by Google/YouTube/Spotify. User is responsible for local law and platform terms.
- No bypass/evasion of DRM, paywalls, or access controls. Do not represent subscription-only or unavailable content as playable.

## Testing and acceptance strategy

- Unit tests: catalog response mapping (including malformed/empty results), resolver success/expiry/failure, Spotify metadata mapping, deterministic match scoring and ambiguous/no-match behavior, queue transitions, sensitive credential redaction/clear semantics.
- Instrumented tests: Compose loading/error/empty states, select-and-queue interactions, service connection, notification/media-session commands, foreground/background continuation, Spotify disconnect behavior.
- Manual device validation: actual YouTube stream playback, interruption/audio focus, screen lock, notification actions, process/service lifecycle, and Spotify WebView flow. No emulator-only test can fully validate protected/short-lived media streams.
- Build and test using JDK 25 and the project Gradle wrapper. Because JDK 25 is installed at drive root `D:\` and Java reports drive-relative `java.home`, invoke Gradle with working directory `D:\` and `-p D:\KULIAH\OpenSource\Musicx`; do not change permanent environment settings as part of feature work without explicit request.

## Licensing and attribution

Target GPLv3 for Musicx. BitChord is GPLv3 and may be studied as a reference; this design assumes no copied source code. Any future direct code reuse or dependency adoption must be reviewed for license compatibility, copyright notices, attribution, and source-distribution obligations before inclusion.

## Explicitly out of scope for Milestone 1

Spotify integration, Google Sign-In, offline downloads, local library scanning, synchronized lyrics, dynamic artwork colors, equalizer, crossfade/automix, Last.fm/ListenBrainz, Discord presence, and release signing. These stay in the later milestones above, not as fake/demo controls in production flows.
