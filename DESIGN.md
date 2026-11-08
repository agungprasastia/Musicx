# Design System

<!-- impeccable:design-schema 1 -->

## Visual Authority & Aesthetic
Derived from `.superpowers/brainstorm` (`editorial-home.html` and `now-playing.html`).
Mode: **Operate / Experience**
Identity: **Editorial Dark** — High-contrast dark obsidian canvas, warm copper/terracotta primary accent, rich duotone bento gradients, prominent artwork, and tactile vector media controls.

## Color Scheme
- `MusicxBackground`: `#0D0D0F` (deep obsidian canvas)
- `MusicxSurface`: `#151518` (cards, headers, docked surfaces)
- `MusicxSurfaceVariant`: `#212126` (chips, secondary cards, inputs)
- `MusicxAccent`: `#E89B72` (editorial warm copper / primary action)
- `MusicxAccentVariant`: `#DF8C64`
- `MusicxText`: `#F7F5F2` (primary high-contrast white text)
- `MusicxMuted`: `#9E9DA6` (secondary metadata, meets WCAG 4.5:1 contrast)
- `MusicxOutline`: `#2B2B33`

## Bento Card Gradients (from editorial-home.html)
- Charts / Top Hits: `[#DF8C64, #503448]`
- New Releases / Fresh Music: `[#A3AA7A, #354749]`
- Moods & Genres / Vibe Selector: `[#4A6FA5, #16253D]`
- Trending / Global Mix: `[#8E54E9, #4776E6]`

## Typography & Hierarchy
- Platform: Android Jetpack Compose Material 3 TypeScale
- Greeting / Eyebrow: `labelSmall` (11sp, letterSpacing 1.5sp, semi-bold, muted)
- Section & Screen Headlines: `headlineLarge` / `headlineMedium` (24–30sp, Bold, onBackground)
- Card Titles: `titleMedium` (16sp, SemiBold, onBackground)
- Metadata & Subtitles: `bodyMedium` / `bodySmall` (13–14sp, muted)

## Component Specifications
- **Navigation Bar**: Standard Material 3 `NavigationBar` with `windowInsets = WindowInsets.navigationBars`, custom tonal container (`#121215`), 48dp+ tap targets, vector icons, no unicode glyphs.
- **Docked MiniPlayer**: Floating rounded container (16dp radius), artwork thumbnail (46dp), single-line title + artist metadata, circular play/pause button (44dp).
- **Now Playing Player**: Full-screen art-first hero view (`statusBarsPadding()` + `navigationBarsPadding()`). Prominent centered album art (260dp+, 20dp radius, drop shadow), thin scrubber bar, circular transport buttons (72dp primary play/pause, 52dp previous/next), and clean tabbed sheets for Queue, Lyrics, and Audio Settings.
- **System Insets**: Android `enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark, navigationBarStyle = SystemBarStyle.dark)` to enforce transparent status & navigation bars with light icons even on light-theme host devices.
