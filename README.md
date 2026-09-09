# Parchment

<h1 align="center">Parchment</h1>

<p align="center">
  <strong>A minimal, offline-first PDF library and reader for Android.</strong>
</p>

<p align="center">
  <a href="https://github.com/hndrx67/Parchment/actions"><img src="https://img.shields.io/github/actions/workflow/status/hndrx67/Parchment/android.yml?branch=main&style=flat-square" alt="Build Status"></a>
  <a href="https://github.com/hndrx67/Parchment/releases"><img src="https://img.shields.io/badge/version-1.4.6--beta-blue?style=flat-square" alt="Version"></a>
  <a href="https://github.com/hndrx67/Parchment/stargazers"><img src="https://img.shields.io/github/stars/hndrx67/Parchment?style=flat-square" alt="Stars"></a>
  <a href="https://github.com/hndrx67/Parchment/network/members"><img src="https://img.shields.io/github/forks/hndrx67/Parchment?style=flat-square" alt="Forks"></a>
  <a href="https://github.com/hndrx67/Parchment/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPL%20v2-yellow.svg?style=flat-square" alt="License"></a>
</p>

<p align="center">
  <a href="https://twitter.com/your_handle"><img src="https://img.shields.io/badge/Twitter-1DA1F2?style=flat-square&logo=twitter&logoColor=white" alt="Twitter"></a>
  <a href="https://linkedin.com/in/your_profile"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=flat-square&logo=linkedin&logoColor=white" alt="LinkedIn"></a>
</p>

## Open in Android Studio

1. Open Android Studio and choose **Open**.
2. Select the `Parchment` directory.
3. Allow Gradle Sync to finish. Use Android Studio's bundled JDK 17.
4. Install Android SDK 36 if prompted.
5. Run on a device or emulator with Android 8.0 (API 26) or newer.

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
