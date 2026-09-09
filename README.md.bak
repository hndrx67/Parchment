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

## Open in Android Studio

1. Open Android Studio and choose **Open**.
2. Select the `Parchment` directory.
3. Allow Gradle Sync to finish. Use Android Studio's bundled JDK 17.
4. Install Android SDK 36 if prompted.
5. Run on a device or emulator with Android 8.0 (API 26) or newer.

The included lightweight Gradle bootstrap downloads Gradle 8.13 on the first build. Android Studio may take a few minutes during the first sync.

## Current scope

Metadata and cover editing are implemented. Destructive page editing, annotations, text search inside PDFs, password-protected PDFs, and exporting modified documents are intentionally reserved for the next milestone.

## Device integration checks

Build `:app:assembleDebug :app:assembleDebugAndroidTest`, install both APKs on an emulator, then run:

```text
adb shell am instrument -w org.hndrx.parchment.test/org.hndrx.parchment.FeatureTestRunner
```

The platform test runner includes 18 checks covering database migration, partial duplicate imports, cancellation, failed imports, batch tags, Public Mode, collection membership, refresh, preference persistence, cover replacement/reversion, document-provider storage, palette contrast, foreground reading time, separate profile histories, configuration recovery, full archive round trips, and unsafe archive rejection using isolated test storage.

## Build compatibility

Parchment deliberately uses Compose BOM `2025.12.00` (Compose 1.10), Android Gradle Plugin 8.13.2, Gradle 8.13, and compile SDK 36. Keep these versions together. Compose 1.12 requires compile SDK 37 and Android Gradle Plugin 9.1 or newer.
