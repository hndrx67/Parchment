# Parchment v1.4.6-beta

Welcome to the first beta release of **Parchment**, a minimal, offline-first PDF library and reader for Android. This release focuses on providing a stable, highly customizable, and privacy-centric document management experience.

## Highlights

*   **Offline-First & Privacy-Focused:** No cloud syncing or tracking. Your library lives entirely on your device.
*   **Dynamic Theming:** Personalize your app with six beautiful palettes (Parchment, OLED Black, Mint Green, Haze Purple, Ocean Blue, and Rose) for both light and dark modes.
*   **Local Profiles:** Create separate profiles with unique avatars and greetings. Each profile maintains its own reading history and statistics while sharing the same library.
*   **Smart Library Management:** Features automatic cover generation, powerful search/filtering, and a dedicated "Management Mode" for batch edits.

## What's New

### UI & UX
- Added **Public Mode** to mask sensitive document covers with a privacy placeholder.
- Implemented adjustable library cover widths (80–400 dp) to suit your layout preference.
- Introduced theme-colored navigation transitions and a setting to toggle animations.
- New splash screen featuring current app versioning.

### Reader Engine
- Support for both **Continuous** and **Paged** reading modes.
- Vertical and horizontal scrolling options.
- **Night Reading:** Inverted PDF colors and adjustable reader backgrounds (Light, Dark, Sepia).
- Persisted reading progress across all documents.
- Quick-access settings panel for brightness and orientation adjustments without leaving the document.

### Storage & Recovery
- **Experimental Backup:** Export your configuration and library metadata as JSON, or create a full ZIP archive including PDFs and covers.
- **Smart Import:** Document picker support with duplicate detection and background processing.
- Flexible storage destination selection for new imports.

## Technical Improvements
- Built with **Jetpack Compose 1.10** and **Material 3**.
- Optimized local database performance using **Room**.
- Integrated **DataStore** for reliable preference management.
- Comprehensive instrumentation suite ensuring stability across 18 core feature checks.

## Known Limitations
*   Text search within PDFs is currently in development.
*   Annotations and destructive page editing (delete/reorder) are reserved for future milestones.
*   Password-protected PDF support is coming soon.

---
**Installation:** Download the `app-release.apk` below and install it on any device running Android 8.0 (API 26) or newer.
