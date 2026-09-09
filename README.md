# Parchment

Parchment is a minimal, offline-first PDF library and reader for Android.

Current version: **1.4.6 beta**

## Included

- Look and Feel → Appearance with Parchment, OLED Black, Mint Green, Haze Purple, Ocean Blue, and Rose palettes for light and dark modes
- Collection cards or compact entries with individual colors and icons
- Experimental Recovery: back up/recover configuration as JSON, or PDFs, covers, avatars, and configuration together as ZIP; configuration alone does not transfer PDF files
- Local profiles with usernames, pictures, personalized splash greetings, and separate reading histories and foreground reading time; profiles share the library
- Library search, sorting, favorites filter, tag filters, and grid/list layout persist across launches
- Brief branded splash on fresh launches with the app version
- Settings → About Parchment with hndrx credits and website
- Theme-colored navigation fades and a setting to disable app animations
- Small, medium, large, and custom library cover widths (80–400 dp)
- Tag filtering (matches any selected tag)
- Long-press a PDF to enter Management Mode with a selection highlight and action window; select multiple PDFs to assign tags, add to collections, favorite, or delete
- Compact per-cover action menus scale with grid width; entries show assigned collection names
- Custom cover preview and replacement, with a Revert to original cover action
- Pull down from the top of the library grid or list to refresh entries and covers
- Privacy → Public Mode masks covers tagged Adult Content with a black warning placeholder
- Storage settings show the import destination and let you select a folder; the choice applies to new imports and existing PDFs remain accessible in their original locations
- Dimmed import progress with Abort; staged files are removed on cancellation or failure
- Content-based duplicate checking skips renamed copies and repeats within the same selection while importing new PDFs
- Import one or multiple PDFs through Android's document picker
- Copy imported documents into app storage or a user-selected folder
- Generate the first page as a library cover
- Grid and list layouts
- Search, favorites, and sorting
- Editable title, author, category, tags, notes, and custom cover
- Continuous PDF reading with zoom and page navigation
- Persisted reading progress
- Light and dark themes
- Local Room database
- Global reader and appearance settings stored with DataStore
- Continuous vertical and horizontal reading
- Paged vertical and horizontal reading
- Light, dark, and sepia reader backgrounds
- Fit-width and fit-page display modes
- Optional inverted PDF colors for night reading
- Adjustable page spacing, margins, brightness, tap navigation, fullscreen, orientation, and screen-awake behavior
- Quick reader settings available without leaving the open document

---

## Overview

Parchment is designed to be a lightweight and elegant solution for managing and reading PDF documents on Android. It prioritizes privacy and local-first storage, ensuring your library remains yours. Built with modern Android technologies, it offers a highly customizable experience with multiple color palettes and personalized reading profiles.

## Key Features

### Personalization & UI
- **Dynamic Themes:** Choose from Parchment, OLED Black, Mint Green, Haze Purple, Ocean Blue, and Rose palettes.
- **Adaptive Modes:** Full support for Light, Dark, and Sepia backgrounds.
- **Customizable Library:** Small, medium, large, and custom cover widths (80–400 dp).
- **Personal Profiles:** Separate reading histories, foreground reading time, and personalized greetings for each user.

### Library Management
- **Powerful Search & Filters:** Search by title, author, or tags. Filter by favorites or specific tags.
- **Management Mode:** Long-press to batch edit tags, add to collections, or perform bulk deletions.
- **Auto-Cover Generation:** Automatically generates high-quality covers from the first page of your PDFs.
- **Duplicate Checking:** Smart content-based duplicate detection to keep your library clean.

### Superior Reading Experience
- **Flexible Layouts:** Continuous or paged reading in both vertical and horizontal orientations.
- **Fit Modes:** Fit-to-width and fit-to-page display options.
- **Night Mode:** Inverted colors for comfortable reading in low-light environments.
- **Persistence:** Reading progress is saved automatically across all your documents.

### Privacy & Storage
- **Offline-First:** No cloud syncing required; your data stays on your device.
- **Public Mode:** Instantly mask sensitive covers with a black warning placeholder.
- **Flexible Storage:** Choose your own import destination folder.

## Tech Stack

- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Database:** [Room](https://developer.android.com/training/data-storage/room)
- **Persistence:** [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
- **Language:** [Kotlin](https://kotlinlang.org/)
- **Architecture:** Clean architecture principles with a focus on offline-first reliability.

## Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- JDK 17.
- Android device or emulator running API 26 (Android 8.0) or newer.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/hndrx67/Parchment.git
   ```
2. Open the project in Android Studio.
3. Allow Gradle to sync.
4. Build and run the `:app` module.

## Testing

Run instrumentation tests to verify library features:
```bash
adb shell am instrument -w org.hndrx.parchment.test/org.hndrx.parchment.FeatureTestRunner
```

## Roadmap

- [ ] Annotations and highlights
- [ ] In-document text search
- [ ] Destructive page editing (rotate, delete, reorder)
- [ ] Password-protected PDF support
- [ ] Exporting modified documents

## Project Structure

```text
parchment/
├── app/
│   ├── src/main/java/org/hndrx/parchment/
│   │   ├── data/          # Room database and DataStore persistence
│   │   ├── pdf/           # PDF rendering and document processing
│   │   ├── settings/      # Application preferences and settings
│   │   ├── ui/            # Compose UI, themes, and navigation
│   │   └── MainActivity.kt
│   └── build.gradle.kts   # Module configuration and dependencies
├── gradle/                # Build system wrapper
├── build.gradle.kts       # Project-wide build configuration
└── settings.gradle.kts    # Project settings
```

## License

Parchment is licensed under the **GNU General Public License v2**. See the [LICENSE](LICENSE) file for more details.

---

<p align="center">
  Developed by <a href="https://github.com/hndrx67">hndrx67</a>
</p>
