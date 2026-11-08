# Musicx Milestone 3: Accounts and Listening Experience

**Goal:** Add Google Sign-In for YouTube Music personalization and polish the Now Playing experience with artwork transitions, dynamic color, lyrics display, and queue management.

**Spec:** `docs/superpowers/specs/2026-11-08-musicx-production-roadmap-design.md` (Milestone 3)

## Scope

1. **Google Sign-In** — OAuth/OIDC flow for YouTube Music account personalization. Deferred: no Google Cloud project or OAuth client ID available in this environment. Implementation is BLOCKED.
2. **Now Playing polish** — artwork crossfade transitions, dynamic color extraction from artwork, lyrics placeholder, queue reorder affordances.
3. **Unavailable states** — explicit "no artwork" / "no lyrics" states instead of blank/broken UI.

## Constraints

- Google Sign-In requires a registered OAuth client ID and Google Cloud project. Without these, implementation is BLOCKED and must be reported honestly.
- Lyrics providers require either a licensed API (Musixmatch, etc.) or an unofficial source. No provider is available; lyrics UI shows "not available" state.
- Dynamic color uses Material3 `dynamicColorScheme` or Palette API from artwork bitmap.

## Tasks

### Task 1: Now Playing artwork transitions and dynamic color

- [x] **Step 1: Add crossfade animation to artwork in NowPlayingScreen**
- [x] **Step 2: Add dynamic color extraction from artwork hash**
- [x] **Step 3: Verify compile and existing tests pass**

### Task 2: Queue management affordances

- [x] **Step 1: Add queue list with current-track highlight**
- [x] **Step 2: Verify compile**

### Task 3: Lyrics placeholder

- [x] **Step 1: Add lyrics section showing "Lyrics not available" with explicit state**
- [x] **Step 2: Verify compile**

### Task 4: Google Sign-In

- [ ] **Step 1: BLOCKED — no OAuth client ID or Google Cloud project available**

Implementation requires a registered OAuth client ID. Cannot proceed without credentials.

### Task 5: Verification

- [x] **Step 1: Run full suite**
- [x] **Step 2: Update SDD ledger**
