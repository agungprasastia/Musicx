# Musicx UI Prototype Design

## Goal and scope

Build a polished, native Android UI prototype for Musicx using Jetpack Compose. Visual direction: editorial dark, inspired by the clean presentation of Apple Music and BitChord without copying either product's assets or exact UI. This phase uses local sample content and demo interactions only.

In scope: Home, Search, Library, and art-first Now Playing screens; bottom navigation for Home/Search/Library; persistent mini-player above navigation; sample tracks, albums, artists, and playlists; touch-friendly demo playback, queue, and screen navigation.

Out of scope: YouTube Music access/streaming, actual audio playback, Google Sign-In, background playback, notification or lock-screen controls, downloads, local scanning, synchronized lyrics, smart transitions, equalizer, scrobbling, and Discord presence. The UI must not imply these capabilities are active.

## Visual design

Use an editorial dark palette with near-black background and charcoal surfaces. Use high-contrast white primary text, subdued gray secondary text, and restrained accent colors derived from the artwork sample palette. Prioritize generous spacing, large album artwork, rounded but not excessive surfaces, and clear touch targets.

Home opens with a personalized greeting, featured mixes, and horizontally browsable recently played/recommended content. Search has an obvious query field and sample result categories. Library organizes saved playlists, albums, artists, and songs. Now Playing is art-first: large artwork, title/artist, progress display, prominent playback controls, and affordances for queue and lyrics views. Artwork is static in this prototype; do not simulate synchronized lyrics or real audio progress.

## Navigation and interaction

Bottom navigation switches among Home, Search, and Library. A mini-player persists above the bottom navigation on these screens; selecting it opens Now Playing. Use lightweight Compose state for selected tab and currently selected sample track. Play/pause and queue actions can update demo UI state, but must remain visibly prototype behavior and must not invoke audio or network APIs.

## Implementation boundaries

Keep implementation within existing Compose application structure and installed Material 3/UI dependencies. Avoid adding dependencies. Prefer small composables and local sample models/data. Preserve current Android project configuration unless build constraints require a narrowly justified adjustment. Ensure dark palette does not switch to system light colors.

## Verification

Run existing unit tests and Android debug assemble. Add one small runnable test only if non-trivial behavior warrants it; prioritize build verification for this UI-only stage. Verify the app compiles without network/media integration and inspect diff for accidental changes to generated or local configuration files.

## Deferred work

Real playback and its service/notification lifecycle form a later milestone. Search/catalog integration, Google identity, offline/local library, lyrics timing, playback processing, scrobbling, and presence integrations require separate designs and appropriate API/legal feasibility checks before implementation.
